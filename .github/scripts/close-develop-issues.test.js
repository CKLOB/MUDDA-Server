'use strict';

const assert = require('node:assert/strict');
const test = require('node:test');

const {
  closeLinkedIssues,
  extractLinkedIssueNumbers,
  isTargetPullRequest,
} = require('./close-develop-issues');

const repository = { full_name: 'CKLOB/MUDDA-Server' };
const sameRepositoryHead = { repo: { full_name: 'CKLOB/MUDDA-Server' } };

function createGithub(issues, comments = new Map()) {
  const createdComments = [];
  const updatedIssues = [];
  const github = {
    rest: {
      issues: {
        async get({ issue_number: issueNumber }) {
          const issue = issues[issueNumber];
          if (issue instanceof Error) {
            throw issue;
          }
          if (!issue) {
            const error = new Error('Not Found');
            error.status = 404;
            throw error;
          }
          return { data: issue };
        },
        async listComments() {
          return { data: [] };
        },
        async createComment(params) {
          createdComments.push(params);
          return { data: params };
        },
        async update(params) {
          updatedIssues.push(params);
          return { data: params };
        },
      },
    },
    async paginate(_method, { issue_number: issueNumber }) {
      return comments.get(issueNumber) ?? [];
    },
  };

  return { github, createdComments, updatedIssues };
}

test('extracts official keywords, colon variants, and deduplicates issue numbers', () => {
  assert.deepEqual(
    extractLinkedIssueNumbers(
      'Closes #32, closes #34\nFIXED: #35\nResolves: #32\nclosed #36',
    ),
    [32, 34, 35, 36],
  );
});

test('ignores non-closing references and masked markdown content', () => {
  const fencedCodeBlock = ['```markdown', 'Closes #7', '```'].join('\n');

  assert.deepEqual(
    extractLinkedIssueNumbers(
      [
        'Refs #1',
        'Related #2',
        '#3',
        'Closes #4, #5',
        '`Closes #6`',
        fencedCodeBlock,
        '<!-- Closes #8 -->',
        'https://example.com/Closes%20%239',
        'Closes CKLOB/other-repository#10',
        '    Closes #12',
        'Closes #0',
        'Closes#11',
      ].join('\n'),
    ),
    [4],
  );
});

test('only accepts a merged PR targeting develop in this repository', () => {
  const mergedDevelopPr = {
    base: { ref: 'develop' },
    head: sameRepositoryHead,
    merged: true,
  };
  assert.equal(isTargetPullRequest(repository, mergedDevelopPr), true);
  assert.equal(
    isTargetPullRequest(repository, {
      base: { ref: 'develop' },
      head: sameRepositoryHead,
      merged: false,
    }),
    false,
  );
  assert.equal(
    isTargetPullRequest(repository, {
      base: { ref: 'main' },
      head: sameRepositoryHead,
      merged: true,
    }),
    false,
  );
  assert.equal(
    isTargetPullRequest(
      { full_name: 'someone/another-repository' },
      mergedDevelopPr,
    ),
    false,
  );
  assert.equal(
    isTargetPullRequest(repository, {
      base: { ref: 'develop' },
      head: { repo: { full_name: 'someone/another-repository' } },
      merged: true,
    }),
    false,
  );
});

test('does not use a title reference when the body is empty or null', async () => {
  const { github, updatedIssues } = createGithub({
    1: { state: 'open' },
  });

  const result = await closeLinkedIssues({
    github,
    repository,
    pullRequest: {
      number: 35,
      title: 'Closes #1',
      body: null,
      base: { ref: 'develop' },
      head: sameRepositoryHead,
      merged: true,
    },
  });

  assert.deepEqual(result.issueNumbers, []);
  assert.deepEqual(updatedIssues, []);
});

test('closes open issues once and does not duplicate an existing audit comment', async () => {
  const existingMarker = '<!-- auto-close-develop-pr:35 -->';
  const comments = new Map([[2, [{ body: `already audited ${existingMarker}` }]]]);
  const { github, createdComments, updatedIssues } = createGithub(
    {
      1: { state: 'open' },
      2: { state: 'open' },
      3: { state: 'closed' },
      4: { state: 'open', pull_request: {} },
    },
    comments,
  );

  const result = await closeLinkedIssues({
    github,
    repository,
    pullRequest: {
      number: 35,
      body: 'Closes #1, fixes #2, resolves #1, closes #3, closes #4, closes #5',
      base: { ref: 'develop' },
      head: sameRepositoryHead,
      merged: true,
    },
  });

  assert.deepEqual(result.closed, [1, 2]);
  assert.equal(createdComments.length, 1);
  assert.equal(createdComments[0].issue_number, 1);
  assert.equal(createdComments[0].body.includes('auto-close-develop-pr:35'), true);
  assert.deepEqual(
    updatedIssues.map((issue) => issue.issue_number),
    [1, 2],
  );
});

test('continues after an API failure and reports the failed issue', async () => {
  const { github, updatedIssues } = createGithub({
    1: Object.assign(new Error('rate limited'), { status: 429 }),
    2: { state: 'open' },
  });

  const result = await closeLinkedIssues({
    github,
    repository,
    pullRequest: {
      number: 35,
      body: 'Closes #1, fixes #2',
      base: { ref: 'develop' },
      head: sameRepositoryHead,
      merged: true,
    },
  });

  assert.deepEqual(result.closed, [2]);
  assert.deepEqual(result.failures, [{ issueNumber: 1, reason: 'rate limited' }]);
  assert.deepEqual(
    updatedIssues.map((issue) => issue.issue_number),
    [2],
  );
});
