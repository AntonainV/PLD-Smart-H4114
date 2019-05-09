
$(document).ready(function(){
    $('#theMap').load("map.html");
    $("#namePart").html("Onglet Map");
    $('#theMap').show();
    getLocation();
 });

function changeToMap() {
    document.getElementsByClassName("active")[0].classList.remove("active");
    document.getElementById("mapLink").classList.add("active");
    $("#namePart").html("Onglet Map");
    getLocation();
    $('#theMap').show();
}

function changeToStream() {
    document.getElementsByClassName("active")[0].classList.remove("active");
    document.getElementById("stream").classList.add("active");
    $('#otherDiv').load("stream.html");
    $('#theMap').hide();
}

function changeToVote() {
    document.getElementsByClassName("active")[0].classList.remove("active");
    document.getElementById("vote").classList.add("active");
    $('#otherDiv').load("vote.html");
    $('#theMap').hide();
}