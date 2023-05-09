const configuration = {
  iceServers: [
    {
      urls: [
        'turn:101.33.202.213:3478?transport=tcp',
        'turn:101.33.202.213:3478?transport=udp'
      ],
      username: 'cys',
      credential: '000001',
    },
    {
      urls: [
        'stun:101.33.202.213:3478',
      ]
    },
  ],
  iceCandidatePoolSize: 10,
}

const RTC_CMD_CREATE = "create"

const RTC_CMD_CLOSE = "close"
const RTC_CMD_ERROR = "error"

const RTC_CMD_JOIN = "join"
const RTC_CMD_REQUEST = "request"
const RTC_CMD_RESPONSE = "response"
const RTC_CMD_LEAVE = "leave"
const RTC_CMD_UPDATE = "update"

const RTC_CMD_RESP_JOIN = "resp_join"
const RTC_CMD_NEW_PEER = "new_peer"
const RTC_CMD_PEER_LEAVE = "peer_leave"

const RTC_CMD_OFFER = "offer"
const RTC_CMD_ANSWER = "answer"
const RTC_CMD_CANDIDATE = "candidate"

const RTC_CMD_PING = "ping"
const RTC_CMD_PONG = "pong"

const webSocketUrl = 'wss://10.119.85.173:18080/vm/ws/' // 本地地址，需要修改为服务器信令地址

let peerConnection = null
let remoteUid = null
let localStream = null
let remoteStream = null
let roomId = null

const heartbeatInterval = 30 * 1000 // 每隔 270 秒发送一次心跳
let currentTimeoutCount = 0 // 当前超时次数
const timeoutCount = 2 // 超时 2 次默认已经断线
let cancelable = []

let ws = null

const uid = Math.random().toString(36).substring(2)

function detectWebRTC() {
    const WEBRTC_CONSTANTS = ['RTCPeerConnection', 'webkitRTCPeerConnection', 'mozRTCPeerConnection', 'RTCIceGatherer']

    const isWebRTCSupported = WEBRTC_CONSTANTS.find((item) => {
        return item in window
    })

    const isGetUserMediaSupported = navigator && navigator.mediaDevices && navigator.mediaDevices.getUserMedia

    if (!isWebRTCSupported || typeof isGetUserMediaSupported === 'undefined') {
        return false
    }

    return true
}

function webSocketConnect(identifier, handler) {
    return new Promise(function(resolve, reject) {
        const ws = new WebSocket(webSocketUrl + `/${identifier}`)
        ws.onopen = () => {
            initHeartbeat()
            resolve(ws)
        }
        ws.onerror = (err) => {
            console.log(`客户端 ${identifier} webSocket 错误 ${err}`)
            reject(err)
        }
        ws.onclose = () => {
            deinitHeartbeat()
            console.log(`客户端 ${identifier} webSocket 关闭`)
            close()
        }
        ws.onmessage = async (message) => await handler(message)
    })
}

async function initWs() {
    if (ws) return
    ws = await webSocketConnect(uid, webSocketHandler)
}

async function openUserMedia(video, audio) {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        console.log("未能打开摄像头或麦克风")
        return
    }
    const stream = await navigator.mediaDevices.getUserMedia({ video: video, audio: audio })
    localStream = stream
    document.querySelector('#localVideo').srcObject = localStream
    remoteStream = new MediaStream()
    document.querySelector('#remoteVideo').srcObject = remoteStream
}

function newConnection() {
    peerConnection = new RTCPeerConnection(configuration)
    registerPeerConnectionListeners()
    if (localStream) {
        localStream.getTracks().forEach(track => peerConnection.addTrack(track, localStream))
    } else {
        console.log("未能打开摄像头或麦克风")
    }
}

async function createOffer() {
    if (peerConnection == null) {
        console.log("客户端", uid, "createOffer error: peerConnection 为空")
        return
    }
    const offer = await peerConnection.createOffer()
    peerConnection.setLocalDescription(offer)
    return offer
}

async function createAnswer() {
    if (peerConnection == null) {
        console.log("客户端", uid, "createAnswer error: peerConnection 为空")
        return
    }
    const answer = await peerConnection.createAnswer()
    peerConnection.setLocalDescription(answer)
    return answer
}

function createRoom() {
    const jsonMsg = {
        'eventCode': RTC_CMD_CREATE,
        "data":{
            'userId': uid
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

async function request() {
    if (peerConnection) {
        alert("请先挂断")
        return
    }
    remoteUid = prompt("请输入拨打的用户id")
    if (!remoteUid) return
    await openUserMedia(true, false)

    const jsonMsg = {
        'eventCode': RTC_CMD_REQUEST,
        "data":{
            'roomNumber': roomId,
            'userId': uid,
            'remoteUserId': remoteUid,
            'extData': {'video': 1, 'audio': 1}
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

function joinRoom(rId) {
    newConnection()
    roomId = rId
    const jsonMsg = {
        'eventCode': RTC_CMD_JOIN,
        "data":{
            'userId': uid,
            'roomNumber': roomId,
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

function leaveRoom() {
    const jsonMsg = {
        'eventCode': RTC_CMD_LEAVE,
        "data":{
            'userId': uid,
            'roomNumber': roomId
        }
    }
    ws.send(JSON.stringify(jsonMsg))
    roomId = null
    remoteUid = null
    close()
}

function updateMediaType(video, audio) {
    const jsonMsg = {
        'eventCode': RTC_CMD_UPDATE,
        "data":{
            'roomNumber': roomId,
            'userId': uid,
            'rtcData': '',
            'extData': {'video': video ? 1 : 0, 'audio': audio ? 1 : 0}
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

function registerPeerConnectionListeners() {
    peerConnection.onconnectionstatechange = () => {
        console.log("客户端", uid, `onconnectionstatechange: ${peerConnection.connectionState}`)
        if (peerConnection.connectionState === 'failed') {
            peerConnection.restartIce()
        }
    }
    peerConnection.onicegatheringstatechange = () => {
        console.log(
            "客户端", uid, `onicegatheringstatechange: ${peerConnection.iceGatheringState}`)
    }

    peerConnection.onsignalingstatechange = () => {
        console.log(
            "客户端", uid, `onsignalingstatechange: ${peerConnection.signalingState}`)
    }

    peerConnection.oniceconnectionstatechange = () => {
        console.log(
            "客户端", uid, `onsignalingstatechange: ${peerConnection.iceConnectionState}`)
    }

    peerConnection.onicecandidate = (event) => {
        if (!event.candidate) {
            console.log("客户端", uid, '获取到最后的 candidate')
            return
        }
        console.log("客户端", uid, '获取 candidate', event.candidate)
        /**
         * candidate 转发 candidate sdp
         * candidate
         * const jsonMsg = {
         *  'cmd': RTC_CMD_CANDIDATE,
         *  'roomId': roomId,
         *  'uid': localUserId,
         *  'remoteUid': remoteUid,
         *  'msg': JSON.stringify(candidateJson)
         * }
         */
        const jsonMsg = {
            'eventCode': RTC_CMD_CANDIDATE,
            "data":{
                'roomNumber': roomId,
                'userId': uid,
                'remoteUserId': remoteUid,
                'rtcData': event.candidate
            }
        }
        ws.send(JSON.stringify(jsonMsg))
    }

    peerConnection.ontrack = (event) => {
        event.streams[0].getTracks().forEach(track => {
            remoteStream.addTrack(track)
        })
    }
}

async function webSocketHandler(message) {
    const json = JSON.parse(message.data)
    console.log("客户端", uid, " webSocket onmessage: ", json)
    switch (json.eventCode) {
        case RTC_CMD_CREATE:
            await handleCreateMessage(json.data)
            break
        case RTC_CMD_REQUEST:
            await handleRequestMessage(json.data)
            break
        case RTC_CMD_UPDATE:
            handleUpdateMessage(json.data)
            break
        case RTC_CMD_RESPONSE:
            handleResponseMessage(json.data)
            break
        case RTC_CMD_NEW_PEER:
            handleNewPeerMessage(json.data)
            break
        case RTC_CMD_RESP_JOIN:
            handleRespJoinMessage(json.data)
            break
        case RTC_CMD_OFFER:
            const answer = await handleOfferMessage(json.data)
            ws.send(JSON.stringify(answer))
            break
        case RTC_CMD_ANSWER:
            await handleAnswerMessage(json.data)
            break
        case RTC_CMD_PEER_LEAVE:
            handlePeerLeaveMessage(json.data)
            break
        case RTC_CMD_CANDIDATE:
            const candidateJson = JSON.parse(json.rtcData)
            const candidate = new RTCIceCandidate(candidateJson)
            await peerConnection.addIceCandidate(candidate)
            break
        case RTC_CMD_ERROR:
            alert(`发生异常`)
            close()
            break
        case RTC_CMD_PONG:
            handlePongMessage(json)
            break
        case RTC_CMD_CLOSE:
            close()
            break;
        default:
            break
    }
}

async function handleCreateMessage(json) {
    roomId = json.roomNumber
    await request()
}

async function handleRequestMessage(json) {
    console.log(`remoteUid: ${remoteUid} json.remoteUid: ${json.remoteUserId} json.uid: ${json.userId} uid: ${uid}`)
    if (remoteUid && json.remoteUserId !== remoteUid) {
        const jsonMsg = {
            'eventCode': RTC_CMD_RESPONSE,
            "data":{
                'roomNumber': json.roomNumber,
                'userId': uid,
                'remoteUserId': json.remoteUserId,
                'rtcData': {'type': 2, 'msg': '正在繁忙'}
            }
        }
        ws.send(JSON.stringify(jsonMsg))
        return false
    }

    const res = confirm(`用户 ${json.remoteUserId} 请求连接`)
    if (!res) {
        const jsonMsg = {
            'eventCode': RTC_CMD_RESPONSE,
            "data":{
                'roomNumber': json.roomNumber,
                'userId': uid,
                'remoteUserId': json.remoteUserId,
                'rtcData': {'type': 1, 'msg': '拒绝连接'}
            }
        }
        ws.send(JSON.stringify(jsonMsg))
        return false
    }

    remoteUid = json.remoteUserId;
    await openUserMedia(true, false)

    const jsonMsg = {
        'eventCode': RTC_CMD_RESPONSE,
        "data":{
            'roomNumber': json.roomNumber,
            'userId': uid,
            'remoteUserId': json.remoteUserId,
            'rtcData': {'type': 0, 'msg': '接受连接'}
        }
    }
    ws.send(JSON.stringify(jsonMsg))

    joinRoom(json.roomNumber)
    return true
}

function handleResponseMessage(json) {
    console.log(json);
    const roomId = json.roomNumber
    const remoteUid = json.remoteUserId
    const res = json.rtcData;

    switch (res.type) {
        case 0:
            //接通, 加入房间
            joinRoom(roomId)
            break
        case 1:
            //正在繁忙
            alert(`${remoteUid} ${res.msg}`)
            close()
            break
        case 2:
            //拒绝连接
            alert(`${remoteUid} ${res.msg}`)
            close()
            break
        default: break
    }
}

async function handleNewPeerMessage(json) {
    const msg = `用户 ${json.remoteUserId} 加入 ${json.roomNumber}`
    showMessage(msg, "enter")
    const offer = await createOffer()
    const jsonMsg = {
        'eventCode': RTC_CMD_OFFER,
        "data":{
            'roomNumber': json.roomNumber,
            'userId': uid,
            'remoteUserId': json.remoteUserId,
            'rtcData': offer,
            'extData': {'video': 1, 'audio': 1}
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

async function handleRespJoinMessage(json) {
    if (json.otherMembers) {
        const otherMembers = json.otherMembers.split(',')
        otherMembers.forEach(function (rid) {
            const msg = `用户 ${rid} 已连接`
            showMessage(msg, "enter")
        })
    }
}

async function handleOfferMessage(json) {
    const roomNumber = json.roomNumber
    const offer = JSON.parse(json.rtcData)

    const offerDescription = new RTCSessionDescription(offer);
    await peerConnection.setRemoteDescription(offerDescription)
    const answer = await createAnswer()
    const jsonMsg = {
        'eventCode': RTC_CMD_ANSWER,
        "data":{
            'roomNumber': roomNumber,
            'userId': uid,
            'remoteUserId': json.remoteUserId,
            'rtcData': answer
        }
    }
    return jsonMsg
}

async function handleAnswerMessage(json) {
    // const roomId = json.roomId
    // const remoteUid = json.remoteUid
    const answer = JSON.parse(json.msg)

    const answerDescription = new RTCSessionDescription(answer)
    await peerConnection.setRemoteDescription(answerDescription)
}

function handlePeerLeaveMessage(json) {
    if (json.remoteUserId) {
        if (json.remoteUserId === remoteUid) {
            leaveRoom()
        }
        const msg = `用户 ${json.remoteUserId} 已挂断`
        showMessage(msg, "leave")
    }
}

function handleUpdateMessage(json) {
    const roomNumber = json.roomNumber
    const remoteUserId = json.userId
    const extData = json.extData

    // 对应修改 video、audio 状态
}

function close() {
    if (localStream) {
        localStream.getTracks().forEach(track => track.stop())
        document.querySelector('#localVideo').srcObject = null
        localStream = null
    }
    if (remoteStream) {
        remoteStream.getTracks().forEach(track => track.stop())
        document.querySelector('#remoteVideo').srcObject = null
        remoteStream = null
    }

    if (peerConnection) {
        peerConnection.close()
        peerConnection = null
    }

    remoteUid = null
    // document.location.reload(true)
}

function showMessage(str, type) {
    let div = document.createElement("div")
    div.innerHTML = str
    if (type === "enter") {
        div.style.color = "blue"
    } else if (type === "leave") {
        div.style.color = "red"
    }
    document.body.appendChild(div)
}

function handlePongMessage(json) {
    currentTimeoutCount = 0
}

function initHeartbeat() {
    currentTimeoutCount = 0
    let cancelable1 = setInterval(() => {
        const jsonMsg = {
            'eventCode': RTC_CMD_PING,
        }
        ws.send(JSON.stringify(jsonMsg))
    }, heartbeatInterval)

    let cancelable2 = setInterval(() => {
        if (currentTimeoutCount > timeoutCount) {
            ws.close()
        }
        currentTimeoutCount += 1
    }, heartbeatInterval * 2)

    cancelable = [cancelable1, cancelable2]
}

function deinitHeartbeat() {
    cancelable.forEach((c) => clearInterval(c))
    cancelable = []
}

async function init() {
    await initWs()

    const isSupportWebRTC = detectWebRTC()

    if (!isSupportWebRTC) {
        console.log(`webRTC no support`)
        return
    }

    document.querySelector('#callBtn').addEventListener('click', () => createRoom())
    document.querySelector('#handupBtn').addEventListener('click', () => leaveRoom())
    document.querySelector("#currentId").innerHTML = uid
}

init()