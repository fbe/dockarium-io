var app = angular.module("dockarium.util", []);

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
