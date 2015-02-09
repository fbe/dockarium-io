var app = angular.module("dockarium", ['ui.bootstrap', 'chart.js', 'ui.router']);


app.config(function($stateProvider, $urlRouterProvider){
    $urlRouterProvider.otherwise("/");

    $stateProvider

        .state('welcome', {
            url: '/',
            views: {

                '': {
                    templateUrl: 'assets/partials/welcome.html'
                },

                'dockerConnectionsTable@welcome': {
                    templateUrl: 'assets/partials/dockerconnectionstable.html',
                    controller: 'DockerConnectionsTableCtrl'
                }


            }
        })

        .state('dashboard', {
            url: '/dashboard',
            views: {

                '': {
                    templateUrl: 'assets/partials/dashboard/dashboard.html'
                },

                'eventlog@dashboard': {
                    templateUrl: 'assets/partials/dashboard/eventlogpanel.html',
                    controller: 'EventLogCtrl'
                }
            }
        })


        .state('admin', {
            url: '/admin',
            templateUrl: 'assets/partials/admin/index.html'
        })

});


app.factory('serverConnection', function($log, $rootScope){

    var severConnection = {};
    var wsProtocol = 'https:' == document.location.protocol ? 'wss:' : 'ws:';
    var ws = new ReconnectingWebSocket(wsProtocol + '//' + document.location.host + '/websocket');

    var statusIcons = {
        initializing : 'glyphicon-question-sign',
        waiting : 'glyphicon-ok-sign icon-warning',
        receiving : 'glyphicon-ok-sign icon-success',
        error : 'glyphicon-remove-sign icon-danger'
    };

    severConnection.status = {};

    severConnection.status = {
        name: 'connecting',
        icon: 'glyphicon-question-sign'
    };

    var broadcastStatus = function(statusname, statusicon){

        $log.info("Websocket status now: " + status);
        severConnection.status = { name: statusname, icon: statusicon };

        $rootScope.$apply(function () {
            $rootScope.$broadcast('WebSocketStatusChange');
        });

    };

    severConnection.send = function(args){
        $log.debug("[Outgoing Message] " + JSON.stringify(args));
        ws.send(JSON.stringify(args));
    };

    ws.onopen = function () {
        broadcastStatus('connected', statusIcons.receiving);
    };

    ws.onconnecting = function () {
        broadcastStatus('connecting', statusIcons.error);

    };

    ws.onerror = function () {
        broadcastStatus('error', statusIcons.error);

    };

    ws.onclose = function () {
        broadcastStatus('disconnected', statusIcons.error);
    };


    ws.onmessage = function (msgevent) {

        // TODO basic event validation here (contains name..?)
        $rootScope.$apply(function () {
            var msg = JSON.parse(msgevent.data);
            $log.debug("[Incoming Message] " + JSON.stringify(msg))
            $rootScope.$broadcast('serverEvent', msg);
            $log.debug("broadcasted " + JSON.stringify(msg))
        });

    };

    return severConnection;
});


app.filter('eventStatusIconClass', function(){

    var eventIconMapping = {};

    eventIconMapping["die"] = "fa-exclamation-triangle";
    eventIconMapping["start"] = "fa-toggle-on";
    eventIconMapping["create"] = "fa-cube";
    eventIconMapping["destroy"] = "fa-trash";
    eventIconMapping["stop"] = "fa-power-off";
    eventIconMapping["pause"] = "fa-pause";
    eventIconMapping["unpause"] = "fa-play";
    eventIconMapping["restart"] = "fa-refresh";

    return function(input){
        return eventIconMapping[input];
    }
});


app.controller("AuthenticationCtrl", function($scope, $log, $modal, serverConnection){


    $scope.$on('serverEvent', function(eventName, payload){

        if(payload.name == "AuthenticationRequired"){

            if(typeof(Storage) !== "undefined" && localStorage.username && localStorage.password) {
                $log.info("trying to authenticate using localstorage data");

                serverConnection.send({command: "authenticate", payload: {
                    username: localStorage.username,
                    password: localStorage.password
                }});

            } else {
                $log.info("authentication required - showing login modal");
                $scope.open();
            }

        }
    });

   $scope.loginUserName = "Felix";
   $scope.items = ['item1', 'item2', 'item3'];

    $scope.open = function () {

        var modalInstance = $modal.open({
            templateUrl: '/assets/partials/authentication/authenticationoverlay.html',
            controller: 'AuthenticationWindowCtrl',
            backdrop:false,
            keyboard:false,
            resolve: {
                items: function () {
                    return $scope.items;
                }
            }
        });

        modalInstance.result.then(function (selectedItem) {
            $scope.selected = selectedItem;
        }, function () {
            $log.info('Modal dismissed at: ' + new Date());
        });
    };

});


app.controller('AuthenticationWindowCtrl', function ($scope, $modalInstance, items, $log, serverConnection) {

    $scope.signIn = function(){

        serverConnection.send({command: "authenticate", payload: {
            username: $scope.username,
            password: $scope.password
        }});

    };

    $scope.$on('serverEvent', function(eventName, payload){
        if(payload.name == "AuthenticationSuccessful"){
            $modalInstance.close();

            // FIXME security - MD5 hash!

            if(typeof(Storage) !== "undefined") {
                localStorage.setItem("username", $scope.username);
                localStorage.setItem("password", $scope.password);
            } else {
                $log.warn("Not saving credentials to the webstorage because of ")
            }

        }

    });






});




app.controller("DockerConnectionsTableCtrl", function($scope, $log, serverConnection){
    $log.info("DockerConnectionsTableCtrl loaded");

    serverConnection.send({command: "getAllDockerConnections"});

    $scope.connections = {};

    $scope.$on('serverEvent', function(eventName, payload){
        if(payload.name == "dockerConnections"){
            $scope.connections = payload.payLoad
        }
    });

});


app.controller("EventLogCtrl", function($scope, $log){

    $scope.eventlog = [];

    $scope.$on('serverEvent', function(eventName, payload){

        $log.debug("EventLogCtrl received an serverEvent, eventName:" + eventName);

        if (payload.name == "event") {

            $log.debug("Payload is an docker event, adding it to the event log");

            $scope.eventlog.push(payload.payLoad);

            if ($scope.eventlog.length > 15) {
                $scope.eventlog.shift();
            }
        }


    });

});

app.controller("AdminCtrl", function($scope, $log, serverConnection){

    $scope.dockerhost = {};

    $scope.saveDockerConnection = function(){
        serverConnection.send({command: "saveDockerConnection", payload: $scope.dockerhost});
    }

});

app.controller("WebsocketStatusCtrl", function($scope, $log, serverConnection){

    $scope.status = serverConnection.status;

    $scope.$on('WebSocketStatusChange', function(){
        $scope.status = serverConnection.status;
    });

});



app.controller("ServerInfoCtrl", function($scope, $log, serverConnection) {

    serverConnection.send({command: "getServerInfo"});

    $scope.serverInfo = {};

    $scope.$on('serverEvent', function(event, msg){

        $log.debug("ServerInfoCtrl received an event");

        if (msg.name == "serverInfo") {
            $scope.serverInfo = msg.payLoad;
        }

    });

});

app.controller("ServerVersionCtrl", function($scope, $log, serverConnection) {

    serverConnection.send({command: "getServerVersion"});

    $scope.$on('serverEvent', function(event, msg){

        if (msg.name == "serverVersion"){
            $scope.serverVersion = msg.payLoad;
        }
    });

});


app.controller("DashboardCtrl", function($scope, $rootScope, $log, serverConnection) {

    serverConnection.send({command: "getMemInfo"});

    $scope.$on('serverEvent', function(eventName, msg){

        if (msg.name == "meminfo"){

            $scope.meminfo = msg.payLoad;
            m=$scope.meminfo;

            $scope.memory = [(m.MemTotal-m.MemAvailable)/1024,m.MemAvailable/1024];

            $scope.memorydetailslabels = [];
            mem = [];
            $scope.memorydetails = [];

            for (var key in $scope.meminfo) {
                if ($scope.meminfo.hasOwnProperty(key) && key != "VmallocChunk" && key != "VmallocTotal") {
                    $scope.memorydetailslabels.push(key);
                    mem.push($scope.meminfo[key]/1024);
                }
            }

            $scope.memorydetails = [mem];

        }

    });

    //Chart.defaults.global.colours

    $scope.labels = ["Memory Used", "Free Memory"];
    $scope.memory = [];
    $scope.colours = [
        { // dark grey
            fillColor: "rgba(77,83,96,0.2)",
            strokeColor: "rgba(77,83,96,1)",
            pointColor: "rgba(77,83,96,1)",
            pointStrokeColor: "#fff",
            pointHighlightFill: "#fff",
            pointHighlightStroke: "rgba(77,83,96,1)"
        },
        { // grey
            fillColor: "rgba(148,159,177,0.2)",
            strokeColor: "rgba(148,159,177,1)",
            pointColor: "rgba(148,159,177,1)",
            pointStrokeColor: "#fff",
            pointHighlightFill: "#fff",
            pointHighlightStroke: "rgba(148,159,177,0.8)"
        }

    ];


    $scope.$on('WebSocketStatusChange', function(){
        $log.info("Status is " + serverConnection.status.name);
        if(serverConnection.status.name == 'connected') {
            $log.info("Sending refreshs");
            // TODO each component must send its own refresh
            serverConnection.send({command: "getMemInfo"});
        }
    });


});

