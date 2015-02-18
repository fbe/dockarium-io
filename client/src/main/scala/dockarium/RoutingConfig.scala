package dockarium

import com.greencatsoft.angularjs.{inject, Config}
import com.greencatsoft.angularjs.extensions.{View, State, UrlRouterProvider, StateProvider}

object RoutingConfig extends Config {

  @inject
  var stateProvider: StateProvider = _

  @inject
  var urlRouteProvider: UrlRouterProvider = _

  override def initialize(): Unit = {
    urlRouteProvider.otherwise("/")


    stateProvider.state(

      "welcome", State(

        url = "/",
        views = Map(

          "" -> View(templateUrl = "assets/partials/welcome.html", controller = ""),

          "dockerConnectionsTable@welcome" -> View(
            templateUrl = "assets/partials/dockerconnectionstable.html",
            controller = "DockerConnectionsTableCtrl"
          )

        )
      )
    )

    stateProvider.state(

      "dashboard", State(

        url = "/dashboard/:dockerConnectionName",

        views = Map(
          "" -> View("assets/partials/dashboard/dashboard.html", "DashboardCtrl"),
          "eventlog@dashboard" -> View("assets/partials/dashboard/eventlogpanel.html", "EventLogCtrl")
        )

      )
    )

    stateProvider.state("admin",
        State(url = "/admin", templateUrl = "assets/partials/admin/index.html")
    )


  }
}
