package pl.vestmedia.tennisreferee.data.auth

class InMemoryCourtSessionStore(
    private var session: CourtSession? = null
) : CourtSessionStore {
    override fun current(): CourtSession? = session

    override fun save(session: CourtSession) {
        this.session = session
    }

    override fun clear() {
        session = null
    }
}
