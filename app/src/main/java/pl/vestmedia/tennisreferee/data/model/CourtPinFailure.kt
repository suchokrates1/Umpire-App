package pl.vestmedia.tennisreferee.data.model

sealed class CourtPinFailure(message: String) : Exception(message) {
    class InvalidPin : CourtPinFailure("invalid-pin")
    class Throttled(val retryAfterMinutes: Int) : CourtPinFailure("too-many-attempts:$retryAfterMinutes")
    class Other(val detail: String) : CourtPinFailure(detail)
}
