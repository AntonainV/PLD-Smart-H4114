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



const configuration = {iceServers: [/*{urls: 'stun:stun.l.googke.com:19302'}*/{urls: 'turn:10.43.6.50:3478',credential: 'test',
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

function errorMsg(msg, error) {
    const errorElement = document.querySelector('#errorMsg');
    errorElement.innerHTML += `<p>${msg}</p>`;
    if (typeof error !== 'undefined') {
      console.error(error);
    }
}

function connectStart(number){
    socket = new WebSocket("ws://192.168.137.1:8084/H4114/video/"+number+"/start/" + name);
    socket.onopen = function (event) {
        console.log("/!\\ Connexion serveur");
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
            pcs[pos].setRemoteDescription(desc);
        }else if(json.type === 'join'){
            index = index + 1;
            var mypos = index;
            listeners.push({name:json.name,index:mypos});
            createPeerConnection(mypos);
            console.log("Adding local stream.");
            console.log(localStream);
            localStream.getTracks().forEach(track => pcs[mypos].addTrack(track, localStream));
        }
    };
    socket.onclose = function (event) {
        sendMessage({
            user : 'start',
            type : 'bye'
        });
        console.log("/!\\ Déconnexion serveur");
    };
}

function createPeerConnection(pos) {
    errorMsg("createPeerConnection");
    errorMsg(configuration);
    pcs[pos] = new RTCPeerConnection(configuration);
    errorMsg("createPeerConnection2");

    pcs[pos].onicecandidate = handleICECandidateEvent;
    pcs[pos].ontrack = handleTrackEvent;
    pcs[pos].onnegotiationneeded = handleNegotiationNeededEvent(this);
    pcs[pos].onremovetrack = handleRemoveTrackEvent;
    pcs[pos].oniceconnectionstatechange = handleICEConnectionStateChangeEvent(this);
    pcs[pos].onicegatheringstatechange = handleICEGatheringStateChangeEvent(this);
    pcs[pos].onsignalingstatechange = handleSignalingStateChangeEvent(this);
}

function handleNegotiationNeededEvent(peer) {
    console.log("Sending offer to peer.");
    peer.createOffer().then(function(offer) {
        return peer.setLocalDescription(offer);
    })
    .then(function() {
        sendMessage({
            user : 'start',
            type : 'offer',
            sdp : peer.localDescription
        });
    });
}

function handleICECandidateEvent(event) {
    if (event.candidate) {
        sendMessage({
            user : "start",
            type: "candidate",
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
    console.log("ashoidiasipdfsapfopapasfjasfpasfopaspasfoafjoaspfosajasajsfajsfopasjfodjapsdjopasjdpoj");
    errorMsg("ashoidiasipdfsapfopapasfjasfpasfopaspasfoafjoaspfosajasajsfajsfopasjfodjapsdjopasjdpoj");
    video.srcObject = event.streams[0];
    console.log(event.streams.length);
    console.log(document.getElementById("video").srcObject);
    errorMsg(document.getElementById("video").srcObject.id);
}

function handleRemoveTrackEvent(event) {
    var stream = document.getElementById("videoDest").srcObject;
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

