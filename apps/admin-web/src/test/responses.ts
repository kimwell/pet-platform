export const traceId = 'a'.repeat(32);
export const reply = (data: unknown, status = 200) => new Response(JSON.stringify({ success: true, data, traceId }), { status, headers: { 'Content-Type': 'application/json', 'X-Trace-Id': traceId } });
export const fail = (status = 401, code = 'AUTH_REQUIRED', fieldErrors?: object[]) => new Response(JSON.stringify({ success: false, error: { code, message: '请检查填写内容', ...(fieldErrors ? { fieldErrors } : {}) }, traceId }), { status, headers: { 'Content-Type': 'application/json', 'Retry-After': '17' } });

