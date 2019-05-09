var map;
var positions = [];
var markers = [];
var assemblyTable = [];
var assemblyInterested = null;
var theAssembly = null;

function createPositions(participants) {

    positions= [];
    positions.push({location: {latitude: latitude, longitude: longitude}});
    if (participants.length > 0)
    {
        var id_assembly = participants[0].id_assembly;
        var title = participants[0].title;
        assemblyTable.push({id_assembly,title});
    }
            
    
    if (participants)
    {
        for (var i = 0; i< participants.length; i++)
        {
            var lat = participants[i].latitude;
            var long = participants[i].longitude;
            positions.push({location: {latitude: lat, longitude: long}});
            var id_assembly = participants[i].id_assembly;
            var title = participants[i].title;
            console.log(id_assembly);
            assemblyTable.push({id_assembly,title});
        }
    }

}

function getLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(setPosition);
    } else {
        alert("Geolocation is not supported by this browser");
    }
}

function setPosition(position) {
    latitude = position.coords.latitude;
    longitude = position.coords.longitude;
    
    $.ajax({
        url: './UserServlet',
        method: 'POST',
        data: {
            action: 'getParticipants',
        },
        dataType: 'json',
        error: function () {
            alert("Error while sending new request");
        }
    }).done(function (data) {
        var participants = data.Participants;
        console.log(theAssembly);
        getAssemblyUser()
        if(theAssembly == null)
        {  initMap(latitude, longitude, participants)  ;}
            
        
    });
}

function getAssemblyUser()
{
    $.ajax({
        url: './UserServlet',
        method: 'POST',
        data: {
            action: 'getAssemblySession',
        },
        dataType: 'json',
        error: function () {
            alert("Error while sending new request");
        }
    }).done(function (data) {
        if (!data.exist) 
        {
            theAssembly = null;
        }
        else
        {
            theAssembly = data.Assembly;
        }
    });
}

var latitude;
var longitude;
//var user = getQueryVariable("user");

function newAssembly() {
    document.getElementById("createAssembly").style.display = "block";
}

function closeCreate() {
    document.getElementById("createAssembly").style.display = "none";
}

function createAssembly() {
    $('#message').text("");
    var title = $('#title').val();
    var description = $('#description').val();
    var radio = $('#radio').val();
    if (title == "")
    {
        return;
    }
    var colour = $('#colour').val();
    if (false) { //Verify whether there are errors
        $('#message').text("Failed");
    } else {
        $.ajax({
            url: './UserServlet',
            method: 'POST',
            data: {
                action: 'createAssembly',
                title: title,
                description: description,
                radio: radio,
                colour: colour,
                latitude:latitude,
                longitude:longitude
            },
            dataType: 'json',
            error: function () {
                console.log("Error while sending  assembly request");
            }
        }).done(function (data) {
            console.log(data);
            if(data.created == true)
            {
                theAssembly = data.Assembly;
                assemblyInterested.set(theAssembly.id_assembly,theAssembly);
                document.getElementById("createAssembly").style.display = "none";
                initButtons();
                //getLocalisation();
            } else {
                $('#message').text("Ups we didn't succed to verify assembly creation");
            }
            
            
        });
    }
}
function quitAssembly()
{
    $.ajax({
        url: './ParticipantServlet',
        method: 'POST',
        data: {
            action : 'removeParticipate',
        },
        dataType: 'json'
    }).done(function (data) {
         theAssembly = null;
         initButtons();
    });
}


function initButtons()
{
    $("#rallyDiv").remove();
    if (theAssembly == null)
    {
        var rallyDiv = document.createElement('div');
        var createRallyDiv = document.createElement('div');
        rallyDiv.class = "dropdown";
        rallyDiv.id="rallyDiv";
        var createButton = document.createElement('button');
        createButton.classList.add("btn");
        createButton.classList.add("btn-primary");
        createButton.style.width = '130px';
        console.log(createButton);
        createButton.innerHTML = 'Créer un Rassemblement';
        createButton.onclick = function () {
            newAssembly();
        };
        var joinRallyDiv = document.createElement('div');
        joinRallyDiv.id = "joinRally";
        var joinButton = document.createElement('button');
        joinButton.innerHTML = 'Joindre un Rassemblement';
        joinButton.classList.add("btn");
        joinButton.classList.add("btn-primary");
        joinButton.style.width = '130px';
        console.log(joinButton);
        joinButton.onclick = function () {
            //joinRallyDiv.setAttribute("");
            $("#joinRally").load("joinRally.html");
        };
        createRallyDiv.appendChild(createButton);
        createRallyDiv.appendChild(document.createElement('br'));
        createRallyDiv.appendChild(document.createElement('br'));
        createRallyDiv.appendChild(joinButton);
        rallyDiv.appendChild(createRallyDiv);
        rallyDiv.appendChild(joinRallyDiv);
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(rallyDiv);
    }
    else
    {
        var rallyDiv = document.createElement('div');
        var createRallyDiv = document.createElement('div');
        rallyDiv.id="rallyDiv";
        rallyDiv.class = "dropdown";
        var createButton = document.createElement('button');
        createButton.classList.add("btn");
        createButton.classList.add("btn-primary");
        createButton.style.width = '130px';
        console.log(createButton);
        createButton.innerHTML = 'Quitter le Rassemblement';
        createButton.onclick = function () {
            quitAssembly();
        };
        var joinRallyDiv = document.createElement('div');
        joinRallyDiv.id = "joinRally";
        var joinButton = document.createElement('button');
        joinButton.innerHTML = theAssembly.title;
        joinButton.classList.add("btn");
        joinButton.style.backgroundColor = theAssembly.colour;
        joinButton.classList.add("btn-primary");
        joinButton.style.width = '130px';
        
        createRallyDiv.appendChild(joinButton);
        createRallyDiv.appendChild(document.createElement('br'));
        createRallyDiv.appendChild(document.createElement('br'));
        createRallyDiv.appendChild(createButton);
        rallyDiv.appendChild(createRallyDiv);
        rallyDiv.appendChild(joinRallyDiv);
        map.controls[google.maps.ControlPosition.TOP_LEFT].push(rallyDiv);
    }
}

function initMap(latitude, longitude, participants) {
    var location = {lat: latitude, lng: longitude};
    
   console.log(participants);
    
    map = new google.maps.Map(document.getElementById("map"), {
        zoom: 19,
        scaleControl: false,
        fullscreenControl: false,
        zoomControl: false,
        center: location,
        mapTypeControl: true,
        mapTypeControlOptions: {
            style: google.maps.MapTypeControlStyle.HORIZONTAL_BAR,
            mapTypeIds: ['roadmap', 'satellite'],
            position: google.maps.ControlPosition.LEFT_BOTTOM
        }});
    
    if (participants)
    {
        createPositions(participants);
    }
    //Markers
    var marker0 = new google.maps.Marker({
        position: location,
        map: map,
        icon: {url: "./icons/man-black-user.png"}
    });
    
    for (var i = 0; i < participants.length; i++) {
   
        var colour = participants[i].colour.toString();
        var pinImage = new google.maps.MarkerImage("http://chart.apis.google.com/chart?chst=d_map_pin_letter&chld=%E2%80%A2|" + colour.slice(1),
        new google.maps.Size(21, 34),
        new google.maps.Point(0,0),
        new google.maps.Point(10, 34));
        
        markers[i] = new google.maps.Marker({
            position: {lat: participants[i].latitude, lng:  participants[i].longitude},
            map: map,
            icon:pinImage,
            strokeColor:participants[i].colour,

        });
        
        var s = "u";
        if (participants[i].status == 1)
        {
            s="<h4>&#x1F3A5;</h4>";
        }
        else if (participants[i].status == 2)
        {
            s="<h4>⚖</h4>";
        }
         console.log(s);
            
        attachPseudo(markers[i], "<div style='text-align:center'>"
                +"<h3>"+participants[i].pseudo+ "</h3>"+ s +
                participants[i].title+"</div>");
                
    }
    attachPseudo(marker0, "moi");

    //afficher des icons avec la couleur selon leur groupe
    dbscan();
    initButtons();
}


function dbscan() {
    var dbscanner = jDBSCAN().eps(0.05).minPts(1).distance('HAVERSINE').data(positions);
    //var cluster_centers = dbscanner.getClusters(); 
    var cluster = dbscanner();
    //return cluster;

    var nbicon = 4;
    var clusterUser = cluster[0];
    assemblyInterested = new Map();
    
    console.log("uuuu", assemblyTable);
    
    for (var i = 1; i < assemblyTable.length; i++)
    {
        if(cluster[i] === clusterUser)
        {
            if(!assemblyInterested.has(assemblyTable[i].id_assembly))
            {
                assemblyInterested.set(assemblyTable[i].id_assembly, assemblyTable[i]);
            }
        }
    }

    
    
  /* for (var i = 0; i < positions.length; i++) {
        if (cluster[i] % nbicon == 0) {
            markers[i] = new google.maps.Marker({
                position: {lat: positions[i].location.latitude, lng: positions[i].location.longitude},
                map: map,
                icon: {url: "./icons/man-red-user.png"}
            });
        }
        if (cluster[i] % nbicon == 1) {
            markers[i] = new google.maps.Marker({
                position: {lat: positions[i].location.latitude, lng: positions[i].location.longitude},
                map: map,
                icon: {url: "./icons/man-blue-user.png"}
            });
        }
        if (cluster[i] % nbicon == 2) {
            markers[i] = new google.maps.Marker({
                position: {lat: positions[i].location.latitude, lng: positions[i].location.longitude},
                map: map,
                icon: {url: "./icons/man-green-user.png"}
            });
        }
        if (cluster[i] % nbicon == 3) {
            markers[i] = new google.maps.Marker({
                position: {lat: positions[i].location.latitude, lng: positions[i].location.longitude},
                map: map,
                icon: {url: "./icons/man-pink-user.png"}
            });
        }
    }*/
}

function attachPseudo(marker, pseudo) {
    var infowindow = new google.maps.InfoWindow({
        content: pseudo
    });

    /**
     marker.addListener('click', function() {
     for(var i = 0;i<markers.length;i++) {
     if(markers[i].infowindow!=null)
     markers[i].infowindow.close(map, markers[i]);
     }
     map.setZoom(19);
     map.setCenter(marker.getPosition());
     infowindow.open(marker.get('map'), marker);
     });*/
    // show info window when marker is clicked
    google.maps.event.addListener(marker, 'click', function () {
        map.setZoom(19);
        map.setCenter(marker.getPosition());
        infowindow.open(map, marker);

    });

    google.maps.event.addListener(map, 'click', function () {
        infowindow.close();
    });
}