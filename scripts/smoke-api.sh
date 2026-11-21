#!/usr/bin/env bash
# API 스모크 테스트 (실제 서버 대상). 사용: scripts/smoke-api.sh [BASE_URL]
#   로컬: scripts/smoke-api.sh http://localhost:8080
#   운영: scripts/smoke-api.sh https://neo8.duckdns.org
# 주의: 데모 사용자로 루틴을 하나 만들고 수정 · 삭제한다 (시드 데이터는 건드리지 않음).
set -uo pipefail
BASE="${1:-http://localhost:8080}"
TOKEN="demo-device-token"
ROUTINE="01KB0000000000000000RTN001"
IMAGE="01KB0000000000000000DEF001"
PASS=0; FAIL=0
TMP=$(mktemp -d); trap 'rm -rf "$TMP"' EXIT

check() { # name expected_status actual_status [expected_code body]
  local name="$1" want="$2" got="$3" code="${4:-}" body="${5:-}"
  if [[ "$got" == "$want" && ( -z "$code" || "$body" == *"\"code\":\"$code\""* ) ]]; then
    PASS=$((PASS+1)); printf '  \033[32mPASS\033[0m %-45s %s %s\n' "$name" "$got" "$code"
  else
    FAIL=$((FAIL+1)); printf '  \033[31mFAIL\033[0m %-45s want %s %s, got %s\n      %s\n' "$name" "$want" "$code" "$got" "${body:0:300}"
  fi
}
req() { # method path [curl args...] → sets STATUS, BODY
  local m="$1" p="$2"; shift 2
  STATUS=$(curl -s -o "$TMP/body" -w '%{http_code}' -X "$m" "$BASE$p" "$@"); BODY=$(cat "$TMP/body")
}
json() { python3 -c 'import sys,json; d=json.load(sys.stdin); 
for k in sys.argv[1].split("."): d=d[int(k)] if k.isdigit() else d[k]
print(d)' "$1" <<<"$BODY"; }
J=(-H 'Content-Type: application/json')
A=(-H "X-Device-Token: $TOKEN")
card() { printf '{"imageId":"%s","title":"%s","time":"%s"}' "$1" "$2" "$3"; }

echo "== $BASE"
req GET /health;                                   check "헬스체크" 200 "$STATUS"
req GET /swagger-ui/index.html;                    check "Swagger UI" 200 "$STATUS"

echo "-- 이미지"
# 실제 4x3 PNG 생성 (서버가 파일 내용으로 형식 · 해상도를 검사하므로 진짜 이미지여야 함)
python3 - "$TMP/a.png" <<'PY'
import sys, struct, zlib
w, h = 4, 3
raw = b"".join(b"\x00" + b"\xff\x80\x00" * w for _ in range(h))
chunk = lambda t, d: struct.pack(">I", len(d)) + t + d + struct.pack(">I", zlib.crc32(t + d))
open(sys.argv[1], "wb").write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0))
    + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))
PY
req POST /api/images "${A[@]}" -F "file=@$TMP/a.png;type=image/png"; check "업로드" 201 "$STATUS" COMMON201 "$BODY"
MYIMG=$(json result.imageId)
req GET "/api/images/$MYIMG";                      check "업로드한 이미지 조회" 200 "$STATUS"
cmp -s "$TMP/a.png" "$TMP/body" && check "바이트 일치" 1 1 || check "바이트 일치" 1 0
req GET "/api/images/$IMAGE";                      check "기본 이미지 조회" 200 "$STATUS"
printf 'hello' > "$TMP/a.txt"
req POST /api/images "${A[@]}" -F "file=@$TMP/a.txt;type=image/png"; check "이미지로 위장한 텍스트 거절" 400 "$STATUS" IMAGE4002 "$BODY"
: > "$TMP/empty.png"
req POST /api/images "${A[@]}" -F "file=@$TMP/empty.png;type=image/png"; check "빈 파일 거절" 400 "$STATUS" IMAGE4001 "$BODY"
{ cat "$TMP/a.png"; head -c $((12*1024*1024)) /dev/zero; } > "$TMP/big.png"
req POST /api/images "${A[@]}" -F "file=@$TMP/big.png;type=image/png"; check "12MB 거절" 400 "$STATUS" FILE400 "$BODY"
req POST /api/images -F "file=@$TMP/a.png;type=image/png"; check "헤더 없이 업로드" 400 "$STATUS" COMMON400 "$BODY"
req POST /api/images -H 'X-Device-Token: nobody' -F "file=@$TMP/a.png;type=image/png"; check "모르는 토큰" 404 "$STATUS" USER404 "$BODY"
req GET /api/images/01KB0000000000000000ZZZ999;  check "없는 이미지" 404 "$STATUS" IMAGE404 "$BODY"
req GET /api/images/abc;                           check "잘못된 이미지 ID" 400 "$STATUS" COMMON400 "$BODY"

echo "-- 루틴 목록"
req GET /api/routines;                             check "목록 (기본 latest)" 200 "$STATUS" COMMON200 "$BODY"
[[ "$BODY" == *'"hasNext"'* && "$BODY" == *'"routines"'* ]] && check "목록 응답 형식" 1 1 || check "목록 응답 형식" 1 0
for s in latest likes participants; do req GET "/api/routines?sort=$s&size=5"; check "목록 sort=$s" 200 "$STATUS" COMMON200 "$BODY"; done
req GET "/api/routines?sort=popular";              check "잘못된 sort" 400 "$STATUS" COMMON400 "$BODY"
req GET "/api/routines?size=51";                   check "size 초과" 400 "$STATUS" COMMON400 "$BODY"
req GET "/api/routines?authorId=01KB0000000000000000MEM001&size=50"; check "내 루틴 (authorId)" 200 "$STATUS" COMMON200 "$BODY"
OWNERS=$(json result.routines | python3 -c 'import sys,ast;print(sorted({r["owner"]["userId"] for r in ast.literal_eval(sys.stdin.read())}))')
[[ "$OWNERS" == "['01KB0000000000000000MEM001']" ]] && check "authorId 필터 (작성자 1명만)" 1 1 || check "authorId 필터 (작성자 1명만)" 1 0
req GET "/api/routines?authorId=abc";              check "잘못된 authorId" 400 "$STATUS" COMMON400 "$BODY"
req GET "/api/routines?cursor=abc";                check "잘못된 커서" 400 "$STATUS" ROUTINE4001 "$BODY"
for s in latest likes participants; do # 커서를 끝까지 따라가서 중복 없는지
  CUR=""; SEEN=""; DUP=0; N=0
  while :; do
    req GET "/api/routines?sort=$s&size=3${CUR:+&cursor=$CUR}"
    for id in $(json result.routines | python3 -c 'import sys,ast;[print(r["routineId"]) for r in ast.literal_eval(sys.stdin.read())]'); do
      [[ " $SEEN " == *" $id "* ]] && DUP=1; SEEN="$SEEN $id"; N=$((N+1)); done
    CUR=$(json result.nextCursor); [[ "$CUR" == "None" ]] && break
  done
  [[ $DUP -eq 0 && $N -gt 0 ]] && check "커서 전체 순회 sort=$s (${N}개, 중복 없음)" 1 1 || check "커서 전체 순회 sort=$s" 1 0
done

echo "-- 루틴"
req GET "/api/routines/$ROUTINE";                  check "시드 루틴 상세" 200 "$STATUS" COMMON200 "$BODY"
BODY_OK=$(printf '{"title":"스모크 테스트 루틴","content":"소개","cards":[%s,%s]}' \
  "$(card "$MYIMG" "첫 번째 스텝 제목" "07:00")" "$(card "$IMAGE" "두 번째 스텝 제목" "08:00")")
req POST /api/routines "${A[@]}" "${J[@]}" -d "$BODY_OK"; check "루틴 생성" 201 "$STATUS" COMMON201 "$BODY"
RID=$(json result.routineId)
[[ "$(json result.cards.1.order)" == 2 && "$(json result.cards.0.time)" == 07:00 ]] && check "스텝 순서·시간" 1 1 || check "스텝 순서·시간" 1 0
req GET "/api/routines/$RID";                      check "생성한 루틴 조회" 200 "$STATUS"
req GET "/api/routines?size=1";                    [[ "$BODY" == *"$RID"* ]] && check "새 루틴이 최신순 맨 앞" 1 1 || check "새 루틴이 최신순 맨 앞" 1 0
SWAP=$(printf '{"title":"순서 바꾼 루틴","cards":[%s,%s,%s]}' \
  "$(card "$IMAGE" "두 번째 스텝 제목" "08:00")" "$(card "$MYIMG" "첫 번째 스텝 제목" "07:00")" "$(card "$IMAGE" "세 번째 새 스텝" "09:00")")
req PUT "/api/routines/$RID" "${A[@]}" "${J[@]}" -d "$SWAP"; check "수정(순서 교체·추가)" 200 "$STATUS" COMMON200 "$BODY"
[[ "$(json result.cards.0.title)" == "두 번째 스텝 제목" && "$(json result.cards.2.order)" == 3 ]] && check "수정 결과" 1 1 || check "수정 결과" 1 0
LONG=$(python3 -c 'print("가"*500)')
req POST /api/routines "${A[@]}" "${J[@]}" -d "{\"title\":\"소개 500자 루틴\",\"content\":\"$LONG\",\"cards\":[$(card "$IMAGE" "스텝 제목입니다" "07:00")]}"
check "소개 500자 (DB VARCHAR(500))" 201 "$STATUS" COMMON201 "$BODY"
TEN=$(for i in $(seq 1 10); do card "$IMAGE" "스텝 제목 $i 번" "07:00"; echo; done | paste -sd, -)
req POST /api/routines "${A[@]}" "${J[@]}" -d "{\"title\":\"스텝 열 개 루틴\",\"cards\":[$TEN]}"; check "스텝 10개" 201 "$STATUS"
req POST /api/routines "${A[@]}" "${J[@]}" -d "{\"title\":\"짧음\",\"cards\":[$(card "$IMAGE" "짧음" "25:00")]}"; check "검증 실패(제목·시간)" 400 "$STATUS" COMMON400 "$BODY"
req POST /api/routines "${A[@]}" "${J[@]}" -d "{\"title\":\"스텝 없는 루틴\",\"cards\":[]}"; check "스텝 0개" 400 "$STATUS" COMMON400 "$BODY"
req POST /api/routines "${A[@]}" "${J[@]}" -d '{"title":'; check "깨진 JSON" 400 "$STATUS" COMMON400 "$BODY"
req POST /api/routines "${A[@]}" "${J[@]}" -d "{\"title\":\"없는 이미지 루틴\",\"cards\":[$(card 01KB0000000000000000ZZZ999 "스텝 제목입니다" "07:00")]}"; check "없는 이미지로 생성" 404 "$STATUS" IMAGE404 "$BODY"
req PUT "/api/routines/$RID" "${A[@]}" "${J[@]}" -d '{"title":"짧음","cards":[]}'; check "수정 검증 실패" 400 "$STATUS" COMMON400 "$BODY"
[[ "$BODY" == *'"title"'* && "$BODY" == *'"cards"'* ]] && check "수정 오류 필드별 표시" 1 1 || check "수정 오류 필드별 표시" 1 0
req DELETE "/api/routines/$RID" "${A[@]}";         check "삭제" 200 "$STATUS" COMMON200 "$BODY"
req GET "/api/routines/$RID";                      check "삭제 후 조회" 404 "$STATUS" ROUTINE404 "$BODY"
req DELETE "/api/routines/$RID" "${A[@]}";         check "재삭제" 404 "$STATUS" ROUTINE404 "$BODY"
req GET /api/routines/01KB0000000000000000ZZZ999; check "없는 루틴" 404 "$STATUS" ROUTINE404 "$BODY"
req GET /api/routines/abc;                         check "잘못된 루틴 ID" 400 "$STATUS" COMMON400 "$BODY"
req PATCH "/api/routines/$ROUTINE" "${A[@]}";      check "지원 안 하는 메서드" 405 "$STATUS" COMMON405 "$BODY"
req GET /api/nothing;                              check "없는 경로" 404 "$STATUS" COMMON404 "$BODY"

echo "== PASS $PASS / FAIL $FAIL"
[[ $FAIL -eq 0 ]]
