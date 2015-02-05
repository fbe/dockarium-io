var app = angular.module("dockarium", []);

var STATUS = {
    initializing : 'glyphicon-question-sign',
    waiting : 'glyphicon-ok-sign icon-warning',
    receiving : 'glyphicon-ok-sign icon-success',
    error : 'glyphicon-remove-sign icon-danger'
};

app.controller("IndexCtrl", function($scope) {

    $scope.status = {
        udp: {status: 'unknown', icon: 'glyphicon-question-sign'},
        websocket: {status: 'connecting', icon: 'glyphicon-question-sign'}
    };

    var wsProtocol = 'https:' == document.location.protocol ? 'wss:' : 'ws:';
    var ws = new ReconnectingWebSocket(wsProtocol + '//' + document.location.host + '/websocket');
    ws.onopen = function () {
        console.log('ws connected');
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
            if (msg[0] == 'p') {
                var pos = {latitude: msg[1], longitude: msg[2], altitude: msg[3], overGround: msg[4]};

                position = new L.LatLng(pos.latitude, pos.longitude);
                plane.setLatLng(position);

                pos.latitude = pos.latitude.toFixed(3);
                pos.longitude = pos.longitude.toFixed(3);
                $scope.data.position = pos;

                if ($scope.settings.sidebar.altitudeChart && !$scope.settings.fullscreen) {
                    var now = new Date().getTime();
                    altitudeSeries.append(now, pos.altitude);
                    groundSeries.append(now, (pos.altitude - pos.overGround));
                }
            } else if (msg[0] == 'prh') {
                var prh = {pitch: msg[1], roll: msg[2], trueHeading: msg[3]}

                plane.setIconAngle(prh.trueHeading);

                if ($scope.settings.sidebar.artificialHorizon) {
                    artificialHorizon.draw(prh.roll, prh.pitch);
                }

                $scope.data.pitchRollHeading = prh;
            } else if (msg[0] == 's') {
                $scope.data.speed = {indKias: msg[1], trueKtgs: msg[2]}
            } else if (msg[0] == 'u') {
                $scope.status.udp = {status: msg[1], icon: STATUS[msg[1]]};
            } else {
                console.log('in :', msg);
            }

        });
    };

});