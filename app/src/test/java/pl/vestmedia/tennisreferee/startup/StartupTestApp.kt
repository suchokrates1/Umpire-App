package pl.vestmedia.tennisreferee.startup

import pl.vestmedia.tennisreferee.TennisRefereeApp

/** Avoids the production heartbeat during JVM cold-start tests. */
class StartupTestApp : TennisRefereeApp() {
    override fun shouldStartHealthCheck(): Boolean = false
}
