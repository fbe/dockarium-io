var app = angular.module("dockarium", ['ui.bootstrap', 'chart.js']);

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
        $log.info("ServerConnection - sending " + JSON.stringify(args));
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
            $rootScope.$broadcast('serverEvent', msg);
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

app.controller("WebsocketStatusCtrl", function($scope, $log, serverConnection){

    $scope.status = serverConnection.status;

    $scope.$on('WebSocketStatusChange', function(){
        $scope.status = serverConnection.status;
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

app.controller("ServerInfoCtrl", function($scope, $log) {

    $scope.serverInfo = {};

    $scope.$on('serverEvent', function(event, msg){

        $log.debug("ServerInfoCtrl received an event");

        if (msg.name == "serverInfo") {
            $scope.serverInfo = msg.payLoad;
        }

    });

});

app.controller("ServerVersionCtrl", function($scope, $log) {

    $scope.$on('serverEvent', function(event, msg){

        $log.debug("ServerVersionCtrl received an event");

        if (msg.name == "serverVersion"){
            $scope.serverVersion = msg.payLoad;
        }
    });

});


app.controller("IndexCtrl", function($scope, $rootScope, $log, serverConnection) {


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
            serverConnection.send({command: "getServerInfo"});
            serverConnection.send({command: "getServerVersion"});
            serverConnection.send({command: "getMemInfo"});
        }
    });


});

