# korime_scene

Minecraft 1.20.4 Fabric용 추리·범죄 현장 연출 모드입니다.

## 포함 기능

- 열쇠 / 키패드 잠금
- 스마트폰 통화·메시지 기록
- CCTV 증거 기록
- 아이템 이름·설명문 수정기
- 설치형 증거 돋보기
- 증거 보드
- 탐정 장비와 돋보기
- 하얀 경계선과 증거 마커

## 주요 아이템 ID

- 열쇠: `korime_scene:key`
- 키패드: `korime_scene:keypad`
- 스마트폰: `korime_scene:smartphone`
- CCTV: `korime_scene:cctv`
- 아이템 수정기: `korime_scene:item_editor`
- 증거: `korime_scene:evidence`
- 증거 보드: `korime_scene:investigation_board`
- 돋보기: `korime_scene:magnifying_glass`
- 하얀선(가로): `korime_scene:line_horizontal`
- 하얀선(세로): `korime_scene:line_vertical`
- 하얀선(모서리): `korime_scene:line_corner`

## 요구 사항

- Minecraft 1.20.4
- Fabric Loader 0.15.11 이상
- Fabric API 0.97.2+1.20.4 이상
- Java 17

## 빌드

저장소의 `korime_scene` 디렉터리에서 `gradle clean build`를 실행합니다.
GitHub Actions에서도 같은 경로를 빌드하며 결과 JAR은 `korime_scene/build/libs/`에 생성됩니다.

## 라이선스

MIT
