'use strict';

const EXPECTED_OWNER = 'CKLOB';
const EXPECTED_REPOSITORY = 'MUDDA-Server';
const EXPECTED_FULL_NAME = `${EXPECTED_OWNER}/${EXPECTED_REPOSITORY}`;
const MARKER_PREFIX = 'auto-close-develop-pr';
const CLOSING_KEYWORDS =
  'close|closes|closed|fix|fixes|fixed|resolve|resolves|resolved';

function stripNonContent(body) {
  let content = typeof body === 'string' ? body : '';

  content = content.replace(/<!--[\s\S]*?-->/g, ' ');
  content = content.replace(/```[\s\S]*?```/g, ' ');
  content = content.replace(/~~~[\s\S]*?~~~/g, ' ');
  content = content.replace(/^(?: {4}|\t).+$/gm, ' ');
  content = content.replace(/`[^`\r\n]*`/g, ' ');
  content = content.replace(/https?:\/\/[^\s<>()]+/gi, ' ');

  return content;
}

function extractLinkedIssueNumbers(body) {
  const content = stripNonContent(body);
  const pattern = new RegExp(
    `(?:^|[^\\p{L}\\p{N}_-])(?:${CLOSING_KEYWORDS})(?![\\p{L}\\p{N}_-])(?:\\s+|\\s*:\\s*)#([1-9]\\d*)(?!\\d)`,
    'giu',
  );
  const issueNumbers = new Set();

  for (const match of content.matchAll(pattern)) {
    const issueNumber = Number(match[1]);
    if (Number.isSafeInteger(issueNumber) && issueNumber > 0) {
      issueNumbers.add(issueNumber);
    }
  }

  return [...issueNumbers];
}

function isTargetPullRequest(repository, pullRequest) {
  return (
    repository?.full_name === EXPECTED_FULL_NAME &&
    pullRequest?.base?.ref === 'develop' &&
    pullRequest?.merged === true &&
    pullRequest?.head?.repo?.full_name === EXPECTED_FULL_NAME
  );
}

function getMarker(pullRequestNumber) {
  return `<!-- ${MARKER_PREFIX}:${pullRequestNumber} -->`;
}

function getPullRequestUrl(pullRequestNumber) {
  return `https://github.com/${EXPECTED_FULL_NAME}/pull/${pullRequestNumber}`;
}

function errorMessage(error) {
  return error instanceof Error ? error.message : String(error);
}

async function closeLinkedIssues({ github, repository, pullRequest }) {
  const result = {
    skipped: [],
    closed: [],
    failures: [],
  };

  if (!isTargetPullRequest(repository, pullRequest)) {
    result.skipped.push({ reason: 'event does not target a merged develop PR' });
    return result;
  }

  const pullRequestNumber = pullRequest.number;
  if (!Number.isSafeInteger(pullRequestNumber) || pullRequestNumber < 1) {
    result.failures.push({ reason: 'invalid pull request number' });
    return result;
  }

  const issueNumbers = extractLinkedIssueNumbers(pullRequest.body);
  result.issueNumbers = issueNumbers;

  for (const issueNumber of issueNumbers) {
    try {
      const issueResponse = await github.rest.issues.get({
        owner: EXPECTED_OWNER,
        repo: EXPECTED_REPOSITORY,
        issue_number: issueNumber,
      });
      const issue = issueResponse.data;

      if (issue.pull_request) {
        result.skipped.push({ issueNumber, reason: 'target is a pull request' });
        continue;
      }

      if (issue.state !== 'open') {
        result.skipped.push({ issueNumber, reason: 'issue is already closed' });
        continue;
      }

      const marker = getMarker(pullRequestNumber);
      const comments = await github.paginate(github.rest.issues.listComments, {
        owner: EXPECTED_OWNER,
        repo: EXPECTED_REPOSITORY,
        issue_number: issueNumber,
        per_page: 100,
      });
      const alreadyAudited = comments.some((comment) =>
        typeof comment.body === 'string' && comment.body.includes(marker),
      );

      if (!alreadyAudited) {
        await github.rest.issues.createComment({
          owner: EXPECTED_OWNER,
          repo: EXPECTED_REPOSITORY,
          issue_number: issueNumber,
          body: `PR #${pullRequestNumber}가 develop에 병합되어 자동으로 종료되었습니다.\n\n${marker}\n${getPullRequestUrl(pullRequestNumber)}`,
        });
      }

      await github.rest.issues.update({
        owner: EXPECTED_OWNER,
        repo: EXPECTED_REPOSITORY,
        issue_number: issueNumber,
        state: 'closed',
        state_reason: 'completed',
      });
      result.closed.push(issueNumber);
    } catch (error) {
      if (error?.status === 404) {
        result.skipped.push({ issueNumber, reason: 'issue does not exist' });
      } else {
        result.failures.push({ issueNumber, reason: errorMessage(error) });
      }
    }
  }

  return result;
}

async function writeSummary(core, result) {
  if (!core?.summary) {
    return;
  }

  const summary = core.summary
    .addHeading('Develop PR Issue Automation')
    .addRaw(`Closed: ${result.closed.join(', ') || 'none'}\n`)
    .addRaw(`Skipped: ${result.skipped.length}\n`)
    .addRaw(`Failures: ${result.failures.length}\n`);

  if (result.skipped.length > 0) {
    summary.addHeading('Skipped', 2).addTable([
      [{ data: 'Issue', header: true }, { data: 'Reason', header: true }],
      ...result.skipped.map((entry) => [
        String(entry.issueNumber ?? '-'),
        entry.reason,
      ]),
    ]);
  }

  if (result.failures.length > 0) {
    summary.addHeading('Failures', 2).addTable([
      [{ data: 'Issue', header: true }, { data: 'Reason', header: true }],
      ...result.failures.map((entry) => [
        String(entry.issueNumber ?? '-'),
        entry.reason,
      ]),
    ]);
  }

  await summary.write();
}

module.exports = {
  closeLinkedIssues,
  extractLinkedIssueNumbers,
  isTargetPullRequest,
  stripNonContent,
  writeSummary,
};
