# GSMSV 런타임 장애 알림 운영 가이드

이 도구는 배포가 끝난 뒤 GSMSV VM에서 MUDDA 운영 컨테이너의 상태와 오류 신호를 1분 주기로 확인하고 Discord에 요약 알림을 보내기 위한 최소 감시 구성이다. 상태 확인과 알림만 수행하며 컨테이너 재시작, Compose 변경, volume 조작, 자동 복구는 수행하지 않는다.

## 감시 범위

- Compose project `mudda-prod`의 `app`, `postgres`, `redis` 컨테이너 존재·running·healthy 상태
- `http://127.0.0.1:8080/actuator/health/readiness` 응답
- 직전 검사 이후 app restart count 증가
- 최근 검사 구간의 app `ERROR` 로그
- 루트 디스크 사용률과 시스템 메모리 사용률

기본 임계치는 디스크 80% 경고/90% 위험, 메모리 85% 경고/90% 위험, readiness timeout 5초, 반복 알림 억제 10분이다. 설정은 스크립트의 환경변수로 조정할 수 있다. RAM 7.9GB·swap 없음 환경을 고려하지만 swap 생성이나 서버 자원 변경은 하지 않는다.

health failure, 컨테이너 종료, restart 증가를 주요 신호로 삼고 ERROR 로그는 보조 신호로만 사용한다. 동일 fingerprint는 기본 10분 동안 억제하며 장애가 정상으로 돌아오면 복구 알림을 한 번 보낸다. 최초 정상 실행에서는 정상 알림을 보내지 않는다.

## 설치 위치와 권한

저장소의 `ops/runtime/install.sh`를 서버에서 root로 실행하면 감시 파일을 `/usr/local/libexec/mudda-runtime-alert`, systemd unit을 `/etc/systemd/system`, 상태 파일을 `/var/lib/mudda/runtime-alerts`에 설치한다. Docker socket과 root:600 webhook 파일을 읽기 때문에 service는 root로 실행하지만, 읽기 전용 경로·NoNewPrivileges·PrivateTmp·보호 옵션으로 범위를 제한한다. Docker socket 접근 권한은 Docker daemon 전체 제어 권한에 준하므로 별도 운영 계정·호스트의 접근 통제를 유지해야 한다.

Webhook은 GitHub Secret이 아니라 서버 전용 파일로 보관한다.

```text
/etc/mudda/discord-alert.env
owner: root
mode: 600
DISCORD_RUNTIME_WEBHOOK_URL=실제 값
```

저장소의 `ops/runtime/discord-alert.env.example`에는 placeholder만 있다. 실제 URL을 문서·Issue·PR·로그에 붙여넣지 않는다. GitHub Secret은 서버 프로세스가 직접 읽을 수 없으며, 향후 CD가 값을 안전하게 전달할 확장 지점만 남겨 둔다. 배포 알림과 런타임 장애 알림은 서로 다른 webhook 사용을 권장한다.

## systemd 실행과 검증

```bash
sudo ops/runtime/install.sh
sudoedit /etc/mudda/discord-alert.env
sudo chmod 600 /etc/mudda/discord-alert.env
sudo systemctl enable --now mudda-runtime-alert.timer
systemctl status mudda-runtime-alert.timer
systemctl list-timers mudda-runtime-alert.timer
journalctl -u mudda-runtime-alert.service -n 50 --no-pager
```

수동 확인은 `sudo systemctl start mudda-runtime-alert.service`로 수행한다. 저장소 fixture 테스트는 `ops/runtime/tests/test.sh`이며 실제 Docker나 webhook을 호출하지 않는다. `MUDDA_RUNTIME_NO_SEND=true`와 fixture 디렉터리를 사용해 정상·unhealthy·readiness 실패·restart 증가·ERROR·임계치·중복 억제·복구·마스킹을 검증한다.

설정 해제는 `sudo ops/runtime/uninstall.sh`로 수행한다. 이 스크립트는 timer와 설치 파일만 제거하고 webhook·상태 파일은 보존한다. rollback은 unit을 제거하거나 이전 저장소 버전의 감시 파일을 재설치하는 범위이며 운영 컨테이너에는 명령을 보내지 않는다.

Discord는 전체 로그 저장소가 아니라 요약 알림 채널이다. 알림은 최근 일부 로그만 최대 약 2,500자로 정제해 포함하며, password·token·JWT·Authorization·Cookie·AWS key·이메일 등 알려진 패턴을 마스킹한다. 마스킹은 완전한 개인정보 탐지를 보장하지 않으므로 전체 로그는 `docker compose -f /opt/mudda/docker-compose.prod.yml logs` 또는 향후 Loki/Grafana에서 확인한다.

Webhook 미설정·Discord 장애는 감시 로그에 URL 없이 실패로 남고 운영 컨테이너 상태에는 영향을 주지 않는다. 실제 운영 Secret과 서버 통합 검증은 아직 수행하지 않았다.
