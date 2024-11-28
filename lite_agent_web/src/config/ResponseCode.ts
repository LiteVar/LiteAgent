enum ResponseCode {
    S_OK = 200,

    FA_HTTP_400 = 400,
    FA_HTTP_401 = 401,
    FA_HTTP_403 = 403,
    FA_HTTP_404 = 404,
    FA_HTTP_406 = 406,
    FA_HTTP_410 = 410,
    FA_HTTP_422 = 422,
    FA_HTTP_500 = 500,
    FA_HTTP_502 = 502,
    FA_HTTP_503 = 503,
    FA_HTTP_504 = 504,
    AGENT_NOT_FOUND = 30003,
}

export default ResponseCode;
