package pl.vestmedia.tennisreferee.e2e

import androidx.test.core.app.ApplicationProvider
import pl.vestmedia.tennisreferee.TennisRefereeApp

/** Retarget the process container. Pass null to restore the production host. */
fun overrideUmpireBackend(url: String?) {
    ApplicationProvider.getApplicationContext<TennisRefereeApp>().container.overrideBaseUrl(url)
}
