var app = angular.module("dockarium", ['ui.bootstrap']);

var STATUS = {
    initializing : 'glyphicon-question-sign',
    waiting : 'glyphicon-ok-sign icon-warning',
    receiving : 'glyphicon-ok-sign icon-success',
    error : 'glyphicon-remove-sign icon-danger'
};

app.controller("IndexCtrl", function($scope) {

    $scope.eventlog = [];

    $scope.status = {
        websocket: {status: 'connecting', icon: 'glyphicon-question-sign'}
    };


    var wsProtocol = 'https:' == document.location.protocol ? 'wss:' : 'ws:';
    var ws = new ReconnectingWebSocket(wsProtocol + '//' + document.location.host + '/websocket');
    ws.onopen = function () {
        console.log('ws connected');
        ws.send("INFO")
        $scope.$apply(function () {
            $scope.status.websocket = {status: 'connected', icon: STATUS.receiving};
        });
    };
    ws.onconnecting = function () {
        console.log('ws connecting');
        $scope.$apply(function () {
            $scope.status.websocket = {status: 'connecting', icon: STATUS.error};
        });
    };
    ws.onerror = function () {
        console.log('ws error');
        $scope.$apply(function () {
            $scope.status.websocket = {status: 'error', icon: STATUS.error};
        });
    };
    ws.onclose = function () {
        console.log('ws closed');
        $scope.$apply(function () {
            $scope.status.websocket = {status: 'disconnected', icon: STATUS.error};
        });
    };

    ws.onmessage = function (msgevent) {
        $scope.$apply(function () {

            var msg = JSON.parse(msgevent.data);

            console.log(msg)

            $scope.eventlog.push(msg);
            if($scope.eventlog.length > 10) {
                $scope.eventlog.shift();
            }
        });
    };

});