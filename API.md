# API 목록

백엔드 컨트롤러 기준 REST API입니다(2026-11-27 확인). 모든 경로는 `/api`로 시작하고, 요청과 응답 본문은 JSON입니다.

## 인증

- 공개 경로는 회원가입, 로그인, 토큰 갱신, 로그아웃 4개뿐이고, 나머지는 모두 로그인이 필요합니다. 등록되지 않은 경로도 인증이 필요합니다.
- 로그인하면 Access Token을 응답 본문으로, Refresh Token을 HttpOnly 쿠키로 받습니다.
- 인증이 필요한 요청은 `Authorization: Bearer <Access Token>` 헤더를 보냅니다.
- Access Token이 만료되면 `POST /api/users/refresh`로 쿠키의 Refresh Token을 보내 새 Access Token을 받습니다.

## 사용자

| 메서드 | 경로 | 기능 | 응답 |
|---|---|---|---|
| POST | `/api/users/register` | 회원가입 (공개) | 201 |
| POST | `/api/users/login` | 로그인 (공개) | 200 |
| POST | `/api/users/refresh` | Access Token 갱신, 쿠키 사용 (공개) | 200 |
| POST | `/api/users/logout` | 로그아웃, 세션 폐기 (공개) | 204 |
| GET | `/api/users/account` | 계정 조회 | 200 |
| PATCH | `/api/users/account` | 계정(이메일·이름) 수정 | 200 |
| PATCH | `/api/users/password` | 비밀번호 변경. 기존 토큰이 모두 무효가 됨 | 200 |
| GET | `/api/users/profile` | 프로필(성별·생년월일·목표) 조회 | 200 |
| PATCH | `/api/users/profile` | 프로필 수정 | 200 |

## 신체 기록

| 메서드 | 경로 | 기능 | 응답 |
|---|---|---|---|
| POST | `/api/users/body-measurements` | 키·몸무게 기록 저장 | 200 |
| GET | `/api/users/body-measurements` | 전체 이력 | 200 |
| GET | `/api/users/body-measurements/latest` | 최신 기록 (없으면 404) | 200 |
| GET | `/api/users/body-measurements/deleted` | 삭제한 기록 목록 | 200 |
| PATCH | `/api/users/body-measurements/{id}` | 수정 | 200 |
| DELETE | `/api/users/body-measurements/{id}` | 삭제 (복구 가능) | 204 |
| POST | `/api/users/body-measurements/{id}/restore` | 복구 | 200 |

## 식품

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | `/api/foods?keyword=김밥&page=1&size=10` | 식약처 식품영양성분 API 검색 (`size` 1~50, 기본 10) |
| GET | `/api/foods/usage` | 오늘 식품 API 호출 수와 분당·일별 한도 |
| GET | `/api/foods/favorites` | 즐겨찾기 목록 |
| PUT | `/api/foods/favorites` | 즐겨찾기 설정·해제 (`foodCode`, `favorite`) |
| GET | `/api/foods/recent` | 최근 섭취 식품 |

## 섭취 기록

| 메서드 | 경로 | 기능 | 응답 |
|---|---|---|---|
| GET | `/api/intakes?date=YYYY-MM-DD` | 날짜별 기록, 하루 합계, 삭제한 기록 | 200 |
| POST | `/api/intakes` | 검색한 식품으로 기록 (`foodCode`, `eatenDate`, `mealType`, `amount`) | 201 |
| POST | `/api/intakes/manual` | 직접 입력 기록 | 201 |
| PATCH | `/api/intakes/{id}` | 날짜·끼니·섭취량 수정 | 200 |
| PATCH | `/api/intakes/{id}/nutrition` | 음식명·기준 영양정보 수정 | 200 |
| DELETE | `/api/intakes/{id}` | 삭제 (복구 가능) | 204 |
| POST | `/api/intakes/{id}/restore` | 복구 | 200 |

`mealType`은 `BREAKFAST`, `LUNCH`, `DINNER`, `SNACK` 중 하나입니다.

직접 입력 요청 예:

```json
{
  "nutrition": {
    "foodName": "직접 음식",
    "baseAmount": 100,
    "calories": 200,
    "carbohydrate": 30,
    "protein": 10,
    "fat": null
  },
  "unit": "g",
  "eatenDate": "2026-10-01",
  "mealType": "LUNCH",
  "amount": 150
}
```

- 영양값은 `baseAmount`(기준량)에 대한 값이며, 실제 섭취량(`amount`)에 비례해 계산합니다. 모르는 값은 `null`로 보내면 "정보 없음"으로 저장되고 0으로 계산하지 않습니다.
- `PATCH /api/intakes/{id}/nutrition`의 본문은 위 `nutrition` 객체입니다. 영양정보 전체를 교체하며 단위와 섭취량은 바꾸지 않습니다.
- 섭취 응답에는 환산한 영양값 외에 기준 영양값(`baseCalories`, `baseCarbohydrate`, `baseProtein`, `baseFat`)과 직접 입력 여부(`manual`), 영양정보 수정 여부(`nutritionEdited`)가 들어 있습니다.

## 식단 계획

| 메서드 | 경로 | 기능 | 응답 |
|---|---|---|---|
| GET | `/api/meal-plans?date=YYYY-MM-DD` | 날짜별 계획 | 200 |
| POST | `/api/meal-plans` | 계획 생성 (`foodCode`, `plannedDate`, `mealType`, `amount`) | 201 |
| PATCH | `/api/meal-plans/{id}` | 날짜·끼니·양 수정 | 200 |
| DELETE | `/api/meal-plans/{id}` | 삭제 (복구 가능) | 204 |
| POST | `/api/meal-plans/{id}/restore` | 복구 | 200 |
| POST | `/api/meal-plans/{id}/consume` | 섭취 기록으로 옮기기 (계획당 한 번) | 200 |

## 권장량·통계

| 메서드 | 경로 | 기능 |
|---|---|---|
| GET | `/api/nutrition/preferences` | 활동량 조회 |
| PUT | `/api/nutrition/preferences` | 활동량 저장 (`activityLevel`) |
| GET | `/api/nutrition/recommendation?date=YYYY-MM-DD` | 그 날짜의 권장 칼로리·탄수화물·단백질·지방 |
| GET | `/api/statistics?start=YYYY-MM-DD&end=YYYY-MM-DD` | 기간별 날짜별 영양 합계·체중·권장 칼로리 (최대 366일) |

- `activityLevel`은 `SEDENTARY`, `LIGHT`, `MODERATE`, `ACTIVE`, `VERY_ACTIVE` 중 하나입니다.
- 권장량은 그 날짜에 적용 중이던 목표·활동량과 그 날짜 이전의 최신 신체 기록으로 계산합니다. 프로필·활동량·신체 기록이 없거나 만 19세 미만이면 계산할 수 없다는 이유를 함께 돌려줍니다.
- 통계 응답은 `{ start, end, days: [...] }`이고, 날짜별 항목은 아래와 같습니다.

| 필드 | 내용 |
|---|---|
| `date` | 날짜 |
| `records` | 그날 섭취 기록 수 |
| `calories`, `carbohydrate`, `protein`, `fat` | 합계 `value`와 영양 정보가 없어 빠진 기록 수 `missingCount` |
| `weight` | 그날 몸무게 (없으면 `null`) |
| `recommendedCalories` | 그날의 권장 칼로리. 권장량 API의 `calories`와 같은 값이며 계산할 수 없으면 `null` |

## 오류 응답

모든 오류는 `{code, message}` 형식입니다. 입력 검증에 실패하면 `errors: [{field, message}]`가 붙습니다.

```json
{"code":"VALIDATION_ERROR","message":"입력값을 확인해주세요.","errors":[{"field":"height","message":"키는 필수입니다."}]}
```

| 상태 | 코드 |
|---|---|
| 400 | `VALIDATION_ERROR`, `INVALID_PASSWORD` |
| 401 | `UNAUTHORIZED`(토큰 없음·잘못됨), `INVALID_LOGIN`(로그인 실패), `SESSION_EXPIRED`(갱신 토큰 없음·만료·재사용) |
| 403 | `FORBIDDEN`(갱신·로그아웃 요청의 출처 검사 실패) |
| 404 | `NOT_FOUND`(없는 주소), `USER_NOT_FOUND`, `USER_PROFILE_NOT_FOUND`, `BODY_MEASUREMENT_NOT_FOUND`, `FOOD_NOT_FOUND`, `INTAKE_NOT_FOUND`, `MEAL_PLAN_NOT_FOUND` |
| 405 / 415 | `METHOD_NOT_ALLOWED` / `UNSUPPORTED_MEDIA_TYPE` |
| 409 | `DUPLICATE_EMAIL`, `BODY_MEASUREMENT_CONFLICT`, `FOOD_SELECTION_CONFLICT`, `INTAKE_CONFLICT`, `MEAL_PLAN_CONFLICT` |
| 429 | `LOGIN_LIMIT`(로그인 실패 반복, `Retry-After` 최대 600초), `FOOD_API_LIMIT`(식품 API 한도, `Retry-After` 60초) |
| 500 / 502 | `INTERNAL_ERROR`(자세한 내용은 서버 로그에만) / `FOOD_API_ERROR`(식품 API 요청 실패) |

오류 코드 목록의 기준은 `backend/src/main/java/…/global/error/ErrorCode.java`입니다.
