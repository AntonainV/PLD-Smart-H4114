/* 
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

var socket;
var localStream;
var started = false;
var pcs = [];
var listeners = [];
var index = 0;



const configuration = {iceServers: [{urls: 'turn:192.168.1.102:3478',credential: 'test',
    username: 'test'}]};
const constraints = window.constraints = {
  audio: false,
  video: true
};


function start(){
    const video = document.getElementById('video');
    // Get access to the camera!
    if(navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
        // Not adding `{ audio: true }` since we only want video now
        navigator.mediaDevices.getUserMedia(constraints)
        .then(function(stream) {
            //video.src = window.URL.createObjectURL(stream);
            video.srcObject = stream;
            localStream = stream;
            video.play();
        })
        .catch(function(error) {
            /* handle the error */
            if (error.name === 'ConstraintNotSatisfiedError') {
                let v = constraints.video;
                errorMsg(`The resolution ${v.width.exact}x${v.height.exact} px is not supported by your device.`);
            } else if (error.name === 'PermissionDeniedError') {
                errorMsg('Permissions have not been granted to use your camera and ' +
                'microphone, you need to allow the page access to your devices in ' +
                'order for the demo to work.');
            }
            errorMsg(`getUserMedia error: ${error.name}`, error);
        });
    }
}

function stop() {
    const video = document.getElementById('video');
    video.pause();
    video.srcObject = null;
    video.src = "";
    localStream.getTracks().forEach(function(track) {
        track.stop();
    });
}

function stopCaller() {
    const video = document.getElementById('video');
    video.pause();
    video.srcObject = null;
    video.src = "";
    localStream.getTracks().forEach(function(track) {
        track.stop();
    });
    socket.close();
    pcs = [];
    listeners = [];
    index = 0;
    identity = 'none';
}

function errorMsg(msg, error) {
    const errorElement = document.querySelector('#errorMsg');
    errorElement.innerHTML += `<p>${msg}</p>`;
    if (typeof error !== 'undefined') {
      console.error(error);
    }
}

function connectStart(number){
    socket = new WebSocket("wss://192.168.137.1:8443/H4114/video/"+number+"/start/" + name);
    socket.onopen = function (event) {
        identity = 'caller';
        console.log("/!\\ Connexion serveur");
        buttonControl('State4',mode);
    };
    socket.onerror = function (event) {
        console.log(event);
    };
    socket.onmessage = function (event) {
        console.log(event.data);
        var json = JSON.parse(event.data);
        if(json.type === 'answer') {
            console.log(json.sdp);
            var desc = new RTCSessionDescription(json.sdp);
            var pos;
            var i = 0;
            for(i = 0; i < listeners.length; i++){
                if(listeners[i].name === json.name){
                    pos = listeners[i].index;
                }
            }
            console.log(pos);
            pcs[pos].setRemoteDescription(desc);
            console.log(pcs[pos]);
            console.log(localStream);
        }else if(json.type === 'join'){
            var mypos = index;
            listeners.push({name:json.name,index:mypos});
            createPeerConnection(mypos,json.name);
            console.log("Adding local stream.");
            index = index + 1;
        }else if(json.type === 'candidate') {
            var candidate = new RTCIceCandidate(json.candidate);
            var pos;
            var i = 0;
            for(i = 0; i < listeners.length; i++){
                if(listeners[i].name === json.name){
                    pos = listeners[i].index;
                }
            }
            pcs[pos].addIceCandidate(candidate);
            console.log(candidate);
        }
    };
    socket.onclose = function (event) {
        console.log("/!\\ Déconnexion serveur");
    };
}

function createPeerConnection(pos,userName) {
    errorMsg("createPeerConnection");
    errorMsg(configuration);
    pcs[pos] = new RTCPeerConnection(configuration);
    localStream.getTracks().forEach(track => pcs[pos].addTrack(track, localStream));
    errorMsg("createPeerConnection2");

    pcs[pos].onicecandidate = handleICECandidateEvent(userName);
    pcs[pos].ontrack = handleTrackEvent;
    pcs[pos].onnegotiationneeded = handleNegotiationNeededEvent(pcs[pos],userName);
    pcs[pos].onremovetrack = handleRemoveTrackEvent;
    pcs[pos].oniceconnectionstatechange = handleICEConnectionStateChangeEvent(pcs[pos]);
    pcs[pos].onicegatheringstatechange = handleICEGatheringStateChangeEvent(pcs[pos]);
    pcs[pos].onsignalingstatechange = handleSignalingStateChangeEvent(pcs[pos]);
}

function handleNegotiationNeededEvent(peer,userName) {
    console.log("Sending offer to peer.");
    peer.createOffer().then(function(offer) {
        return peer.setLocalDescription(offer);
    })
    .then(function() {
        console.log(peer.localDescription);
        sendMessage({
            user : 'start',
            type : 'offer',
            name : userName,
            sdp : peer.localDescription
        });
    });
}

function handleICECandidateEvent(userName) {
    if (event.candidate) {
        sendMessage({
            user : "start",
            type : "candidate",
            name : userName,
            candidate: event.candidate
        });
    }
}

function handleICEConnectionStateChangeEvent(peer) {
    console.log("*** ICE connection state changed to " + peer.iceConnectionState);

    switch(peer.iceConnectionState) {
        case "closed":
        case "failed":
        case "disconnected":
            break;
    }
}

function handleSignalingStateChangeEvent(peer) {
    console.log("*** WebRTC signaling state changed to: " + peer.signalingState);
    switch(peer.signalingState) {
        case "closed":
            break;
    }
}

function handleICEGatheringStateChangeEvent(peer) {
    console.log("*** ICE gathering state changed to: " + peer.iceGatheringState);
}

function handleTrackEvent(event) {
    const video = document.getElementById('video');
    video.srcObject = event.streams[0];
    console.log(event.streams.length);
    console.log(document.getElementById("video").srcObject);
    errorMsg(document.getElementById("video").srcObject.id);
}

function handleRemoveTrackEvent(event) {
    var stream = document.getElementById("video").srcObject;
    var trackList = stream.getTracks();

    if (trackList.length === 0) {
        closeVideoCall();
    }
}

function sendMessage(message){
    var msgString = JSON.stringify(message);
    console.log('message sent : ' + msgString);
    socket.send(msgString);
}