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
const RTC_CMD_LEAVE = "leave"

const RTC_CMD_RESP_JOIN = "resp_join"
const RTC_CMD_NEW_PEER = "new_peer"
const RTC_CMD_UPDATE = "update"
const RTC_CMD_PEER_LEAVE = "peer_leave"

const RTC_CMD_OFFER = "offer"
const RTC_CMD_ANSWER = "answer"
const RTC_CMD_CANDIDATE = "candidate"

const RTC_CMD_PING = "ping"
const RTC_CMD_PONG = "pong"

const heartbeatInterval = 30 * 1000 // 每隔 270 秒发送一次心跳
let currentTimeoutCount = 0 // 当前超时次数
const timeoutCount = 2 // 超时 2 次默认已经断线
let cancelable = []

const webSocketUrl = 'wss://10.119.85.173:18080/vm/ws/' // 本地地址，需要修改为服务器信令地址

let clients = null
let roomId = null

let ws = null

const localVideo = document.querySelector('#localVideo')
let remoteVideos = document.querySelector('#videos')
let logs = document.querySelector("#logs")

let localStream = null
let shareStream = null
let isShare = false

let mediaRecorder = null

const startRecordBtn = document.querySelector('#startRecordBtn')
const resumeRecordBtn = document.querySelector('#resumeRecordBtn')
const pauseRecordBtn = document.querySelector('#pauseRecordBtn')
const stopRecordBtn = document.querySelector('#stopRecordBtn')
const saveRecordBtn = document.querySelector('#saveRecordBtn')
const recordStatus = document.querySelector('#recordStatus')
const enabledAudioBtn = document.querySelector('#enabledAudioBtn')
const enabledVideoBtn = document.querySelector('#enabledVideoBtn')

let chunks = []
let srcUrl = null

//当前用户Id
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

// 用户id，peerconnection， 页面selector
function Client(uid, conn, selector) {
    this.uid = uid
    this.conn = conn
    this.selector = selector
}

function webSocketConnect(identifier, handler) {
    return new Promise(function(resolve, reject) {
        const ws = new WebSocket(webSocketUrl + `${identifier}`)
        document.getElementById("currentRoom").innerHTML = `uid: ${uid}`
        ws.onopen = () => {
            initHeartbeat()
            resolve(ws)
        }
        ws.onerror = (err) => {
            console.log(`客户端 ${identifier} webSocket 错误 ${err}`)
            setLogs(`客户端 ${identifier} webSocket 关闭`)
            reject(err)
        }
        ws.onclose = () => {
            deinitHeartbeat()
            console.log(`客户端 ${identifier} webSocket 关闭`)
            setLogs(`客户端 ${identifier} webSocket 关闭`)
        }
        //处理服务端消息
        ws.onmessage = async (message) => await handler(message)
    })
}

async function initWs() {
    if (ws) return
    ws = await webSocketConnect(uid, webSocketHandler)
}

async function openUserMedia(video, audio) {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        console.log("打开媒体失败")
        return
    }
    const stream = await navigator.mediaDevices.getUserMedia({ video: video, audio: audio })
    enabledAudioBtn.innerHTML = audio ? '打开音频' : '关闭音频'
    enabledVideoBtn.innerHTML = video ? '打开摄像头' : '关闭摄像头'
    return stream
}

async function getDisplayMedia(video, audio) {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getDisplayMedia) {
        console.log("打开媒体失败")
        return
    }
    const stream = await navigator.mediaDevices.getDisplayMedia({ video: video, audio: audio })
    enabledAudioBtn.innerHTML = audio ? '打开音频' : '关闭音频'
    enabledVideoBtn.innerHTML = video ? '打开摄像头' : '关闭摄像头'
    return stream
}

function newConnection(uid) {
    const conn = new RTCPeerConnection(configuration)
    conn.uid = uid
    registerPeerConnectionListeners(conn)
    return conn
}

async function createOffer(conn) {
    if (conn == null) {
        console.log("客户端", uid, "createOffer error: peerConnection 为空")
        return
    }
    const offer = await conn.createOffer()
    conn.setLocalDescription(offer)
    return offer
}

async function createAnswer(conn) {
    if (conn == null) {
        console.log("客户端", uid, "createAnswer error: peerConnection 为空")
        return
    }
    const answer = await conn.createAnswer()
    conn.setLocalDescription(answer)
    return answer
}

//创建房间
function createRoom() {
    const jsonMsg = {
        'eventCode': RTC_CMD_CREATE,
        'data' : {
            'userId': uid
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

//加入房间
async function joinRoom() {
    if (!roomId) {
        roomId = prompt("请输入房间号")
        if (!roomId) {
            alert("房间号为空")
            return
        }
    }
    document.getElementById("currentRoom").innerHTML = `uid: ${uid} 房间号：${roomId}`

    localStream = await openUserMedia(true, true)
    localVideo.srcObject = localStream

    /**
     * 1、join 加入房间
     * join
     * const jsonMsg = {
     *  'cmd': RTC_CMD_JOIN,
     *  'roomId': roomId,
     *  'uid': localUserId
     * }
     */
    const jsonMsg = {
        'eventCode': RTC_CMD_JOIN,
        'data':{
            'roomNumber': roomId,
            'userId': uid
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

//离开房间
async function leaveRoom() {
    /**
     * leave 离开房间
     * leave
     * const jsonMsg = {
     *  'cmd': 'leave',
     *  'roomId': roomId,
     *  'uid': localUserId
     * }
    */
    const jsonMsg = {
        'eventCode': RTC_CMD_LEAVE,
        'data':{
            'roomNumber': roomId,
            'userId': uid
        }
    }
    ws.send(JSON.stringify(jsonMsg))

    close()
}

async function selectShareScreen(status) {
    isShare = status
    document.querySelector('#stopShareBtn').disabled = !isShare
    document.querySelector('#selectShareBtn').disabled = isShare
    if (status) {
        shareStream = await getDisplayMedia(true, false)
        if (shareStream) {
            //监听手动点击“停止分享”
            shareStream.getVideoTracks()[0].onended = () => selectShareScreen(false)
            clients.forEach(async function (remoteClient) {
                remoteClient.conn.removeStream(localStream)
                remoteClient.conn.addStream(shareStream)

                await switchScreenRetryConnection(remoteClient)
            })
            localVideo.srcObject = shareStream
        }
    } else {
        if (shareStream) {
            clients.forEach(async function (remoteClient) {
                remoteClient.conn.removeStream(shareStream)
                remoteClient.conn.addStream(localStream)

                await switchScreenRetryConnection(remoteClient)
            })
            shareStream.getTracks().forEach((track) => track.stop())
            localVideo.srcObject = localStream
            shareStream = null
        } else {
            localVideo.srcObject = localStream
            shareStream = null
        }
    }
}

//发送本地rtc数据
async function switchScreenRetryConnection(client) {
    const offer = await createOffer(client.conn)
    const jsonMsg = {
        'eventCode': RTC_CMD_OFFER,
        'data':{
            'roomNumber': roomId,
            'userId': uid,
            'remoteUserId': client.uid,
            'rtcData': JSON.stringify(offer)
        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

function close() {
    if (localStream) {
        localStream.getTracks().forEach((track) => track.stop())
        localStream = null
    }

    if (shareStream) {
        shareStream.getTracks().forEach((track) => track.stop())
        shareStream = null
    }

    localVideo.srcObject = null

    if (clients) {
        clients.forEach(function (remoteClient) {
            remoteClient.selector.srcObject.getTracks((track) => track.stop())
            remoteClient.selector.srcObject = null
            remoteVideos.removeChild(remoteClient.selector)
            remoteClient.conn.close()
        })
    }
    clients = null
    roomId = null
    document.getElementById("currentRoom").innerHTML = ""
}

//修改媒体类型
function updateMediaType(video, audio) {
    const jsonMsg = {
        'eventCode': RTC_CMD_UPDATE,
        'data':{'roomId': roomId,
            'userId': uid,
            'rtcData': '',
            'extData': JSON.stringify({'video': video ? 1 : 0, 'audio': audio ? 1 : 0})

        }
    }
    ws.send(JSON.stringify(jsonMsg))
}

function registerPeerConnectionListeners(conn) {
    conn.onconnectionstatechange = () => {
        console.log("客户端", uid, `onconnectionstatechange: ${conn.connectionState}`)
        setLogs(`客户端 ${uid} onconnectionstatechange: ${conn.connectionState}`)
        if (conn.connectionState === 'failed') {
            conn.restartIce()
        }
    }
    conn.onicegatheringstatechange = () => {
        console.log(
            "客户端", uid, `onicegatheringstatechange: ${conn.iceGatheringState}`)
        setLogs(`客户端 ${uid} onicegatheringstatechange: ${conn.iceGatheringState}`)
    }

    conn.onsignalingstatechange = () => {
        console.log(
            "客户端", uid, `onsignalingstatechange: ${conn.signalingState}`)
        setLogs(`客户端 ${uid} onsignalingstatechange: ${conn.signalingState}`)
    }

    conn.oniceconnectionstatechange = () => {
        console.log(
            "客户端", uid, `oniceconnectionstatechange: ${conn.iceConnectionState}`)
        setLogs(`客户端 ${uid} oniceconnectionstatechange: ${conn.iceConnectionState}`)
    }

    conn.onicecandidate = (event) => {
        if (!event.candidate) {
            console.log("客户端", uid, '获取到最后的 candidate')
            return
        }
        console.log("客户端", uid, '获取 candidate', event.candidate)
        setLogs(`客户端 ${uid} 获取 candidate: ${event.candidate}`)
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
            'data':{
                'roomNumber': roomId,
                'userId': uid,
                'remoteUserId': conn.uid,
                'rtcData': JSON.stringify(event.candidate)
            }
        }
        ws.send(JSON.stringify(jsonMsg))
    }

    conn.ontrack = (trackEvent) => {
        console.log(`conn.ontrack ${trackEvent}`)
        // console.log(`conn.ontrack ${conn.uid}`)
        let remoteClient = clients.find((c) => c.uid === conn.uid)
        remoteClient.selector.srcObject = trackEvent.streams[0]
        // 获取音视频轨道对象
        const audioTrack = trackEvent.streams[0].getAudioTracks()[0]
        audioTrack.onended = (event) => {
            console.log(`audioTrack ${audioTrack.id} has ended`)
        }
        audioTrack.onunmute = (event) => {
            console.log(`audioTrack ${audioTrack.id} has onunmute`)
        }
        audioTrack.onmute = (event) => {
            console.log(`audioTrack ${audioTrack.id} has onmute`)
        }
        audioTrack.addEventListener('enabled', () => {
            console.log(`audioTrack ${audioTrack.id} has been ${audioTrack.enabled}`)
        })
        const videoTrack = trackEvent.streams[0].getVideoTracks()[0]
        videoTrack.onended = (event) => {
            console.log(`videoTrack ${videoTrack.id}  has ended`)
        }
        videoTrack.onunmute = (event) => {
            console.log(`videoTrack ${videoTrack.id}  has onunmute`)
        }
        videoTrack.onmute = (event) => {
            console.log(`videoTrack ${videoTrack.id}  has onmute`)
        }
        videoTrack.addEventListener('enabled', () => {
            console.log(`videoTrack has been ${videoTrack.enabled}`)
        })
        // event.streams[0].getTracks().forEach(track => remoteClient.selector.srcObject.addTrack(track))
    }
}

async function webSocketHandler(message) {
    const json = JSON.parse(message.data)
    console.log("客户端", uid, " webSocket onmessage: ", json)
    switch (json.eventCode) {
        //创建房间
        case RTC_CMD_CREATE:
            handleCreateMessage(json)
            break
        // 房间有人加入
        case RTC_CMD_NEW_PEER:
            const offer = await handleNewPeerMessage(json)
            ws.send(JSON.stringify(offer))
            break
        //加入房间
        case RTC_CMD_RESP_JOIN:
            document.getElementById("currentRoom").innerHTML = `uid: ${uid} 房间号：${roomId} 成员：${uid},${json.remoteUserIds}`;
            break
        //接收到其他用户offer
        case RTC_CMD_OFFER:
            const answer = await handleOfferMessage(json)
            ws.send(JSON.stringify(answer))
            break
        //接收到其他用户回答
        case RTC_CMD_ANSWER:
            await handleAnswerMessage(json)
            break
        //更新
        case RTC_CMD_UPDATE:
            handleUpdateMessage(json)
            break
        case RTC_CMD_PEER_LEAVE:
            handlePeerLeaveMessage(json)
            break
        case RTC_CMD_CANDIDATE:
            handleCandidateMessage(json)
            break
        case RTC_CMD_ERROR:
            alert(`${json.msg}`)
            break
        case RTC_CMD_PONG:
            handlePongMessage(json)
            break
        case RTC_CMD_CLOSE:
            close()
            ws.close()
            break
        default:
            break
    }
}

//处理服务端 创建房间 请求
function handleCreateMessage(json) {
    roomId = json.data.roomNumber
    document.getElementById("currentRoom").innerHTML = `房间号：${roomId}`
}

/**
 * new-peer 服务器通知客户端有新人加入，收到new-peer则发起连接请求
 * new-peer
 * const jsonMsg = {
 * 'cmd': RTC_CMD_OFFER,
 * 'roomId': roomId,
 * 'uid': uid,
 * 'remoteUid': remoteUid,
 * 'msg': JSON.stringify(offer)
 * }
 */
async function handleNewPeerMessage(json) {
    const remoteUid = json.data.remoteUserId;
    const conn = newConnection(remoteUid)
    const localStream = localVideo.srcObject
    if (localStream) {
        localStream.getTracks().forEach(track => conn.addTrack(track, localStream))
    }

    const newVideo = document.createElement("video")
    newVideo.muted = false
    newVideo.autoplay = true
    newVideo.playsInline = true
    newVideo.srcObject = new MediaStream()

    const newClient = new Client(remoteUid, conn, newVideo)

    if (clients) {
        clients = [...clients, newClient]
    } else {
        clients = [newClient]
    }

    remoteVideos.appendChild(newVideo)

    const offer = await createOffer(conn)
    const jsonMsg = {
        'eventCode': RTC_CMD_OFFER,
        'data':{
            'roomNumber': roomId,
            'userId': uid,
            'remoteUserId': remoteUid,
            'rtcData': JSON.stringify(offer),
            'extData': JSON.stringify({'video': 1, 'audio': 1})
        }
    }
    console.log(`用户 ${uid} 发送 offer`)
    return jsonMsg
}

/**
 * offer 转发 offer sdp
 * offer
 * var jsonMsg = {
 *  'cmd': RTC_CMD_OFFER,
 *  'roomId': roomId,
 *  'uid': localUserId,
 *  'remoteUid': remoteUid,
 *  'msg': JSON.stringify(sessionDescription)
 * }
 */
async function handleOfferMessage(json) {
    const roomId = json.data.roomNumber;
    const remoteUid = json.data.remoteUserId;
    const offer = json.data.rtcData;

    console.log(`用户 ${uid} 收到 ${remoteUid} offer`)

    let conn = null

    if (clients && (clients.find((c) => c.uid === remoteUid))) {
        const client = (clients.find((c) => c.uid === remoteUid))
        setLogs(`找到 ${remoteUid} client`)
        conn = client.conn
    } else {
        // 如果找不到，就新建 RTCPeerConnection
        setLogs(`如果找不到 ${remoteUid} client，就新建 RTCPeerConnection`)
        conn = newConnection(remoteUid)

        const localStream = localVideo.srcObject
        if (localStream) {
            localStream.getTracks().forEach(track => conn.addTrack(track, localStream))
        }

        const newVideo = document.createElement("video")
        newVideo.muted = false
        newVideo.autoplay = true
        newVideo.playsInline = true
        newVideo.srcObject = new MediaStream()

        const newClient = new Client(remoteUid, conn, newVideo)

        if (clients) {
            clients = [...clients, newClient]
        } else {
            clients = [newClient]
        }

        remoteVideos.appendChild(newVideo)
    }

    const offerDescription = new RTCSessionDescription(offer);
    await conn.setRemoteDescription(offerDescription)
    const answer = await createAnswer(conn)
    const jsonMsg = {
        'eventCode': RTC_CMD_ANSWER,
        'data':{
            'roomNumber': roomId,
            'userId': uid,
            'remoteUserId': remoteUid,
            'rtcData': JSON.stringify(answer)
        }
    }

    console.log(`用户 ${uid} 发送 answer`)
    return jsonMsg
}

/**
 * answer 转发 answer sdp
 * answer
 * var jsonMsg = {
 *  'cmd': RTC_CMD_ANSWER,
 *  'roomId': roomId,
 *  'uid': localUserId,
 *  'remoteUid': remoteUid,
 *  'msg': JSON.stringify(sessionDescription)
 * }
 */
async function handleAnswerMessage(json) {
    // const roomId = json.roomId
    const remoteUid = json.data.remoteUserId;
    const answer = json.data.rtcData;
    if (!clients) {
        return
    }
    const remoteClient = clients.find((c) => c.uid === remoteUid)

    if (remoteClient) {
        const answerDescription = new RTCSessionDescription(answer)
        await remoteClient.conn.setRemoteDescription(answerDescription)
    }
}

async function handleCandidateMessage(json) {
    const remoteUid = json.data.remoteUserId
    if (!clients) {
        return
    }
    const remoteClient = clients.find((c) => c.uid === remoteUid)

    if (remoteClient) {
        const candidateJson = json.data.rtcData;
        const candidate = new RTCIceCandidate(candidateJson)
        await remoteClient.conn.addIceCandidate(candidate)
    }
}

/**
 * peer-leave 服务端通知客户端有人离开
 * peer-leave
 * var jsonMsg = {
 *  'cmd': RTC_CMD_PEER_LEAVE,
 *  'remoteUid': remoteUid
 * }
 */
function handlePeerLeaveMessage(json) {
    const remoteUid = json.data.remoteUserId;
    if (!clients) {
        return
    }
    const remoteClient = clients.find((c) => c.uid === remoteUid)

    if (remoteClient) {
        remoteClient.selector.srcObject.getTracks((track) => track.stop())
        remoteClient.selector.srcObject = null
        remoteVideos.removeChild(remoteClient.selector)
        remoteClient.conn.close()
        clients = clients.filter((c) => c.uid !== remoteUid)

        console.log(`用户 ${remoteUid} 退出`)
    }

    const lastMembers = clients.map((m) => m.uid).join(',')
    document.getElementById("currentRoom").innerHTML = `uid: ${uid} 房间号：${roomId}`
}

function handleUpdateMessage(json) {
    const roomId = json.data.roomNumber
    const remoteUid = json.data.userId
    const ext = json.data.extData;

    // 对应修改 video、audio 状态
}

function setLogs(log) {
    logs.innerHTML += `${log}<br/>`
}

function toggleAudioAction() {
    localStream.getAudioTracks()[0].enabled = !localStream.getAudioTracks()[0].enabled
    const isEnabled = localStream.getAudioTracks()[0].enabled
    enabledAudioBtn.innerHTML = !isEnabled ? '打开音频' : '关闭音频'
}

function toggleVideoAction() {
    localStream.getVideoTracks()[0].enabled = !localStream.getVideoTracks()[0].enabled
    const isEnabled = localStream.getVideoTracks()[0].enabled
    enabledVideoBtn.innerHTML = !isEnabled ? '打开摄像头' : '关闭摄像头'
}

async function init() {
    await initWs()

    const isSupportWebRTC = detectWebRTC()

    if (!isSupportWebRTC) {
        console.log(`webRTC no support`)
        return
    }

    document.querySelector('#createBtn').addEventListener('click', () => createRoom())
    document.querySelector('#joinBtn').addEventListener('click', async () => await joinRoom())
    document.querySelector('#leaveBtn').addEventListener('click', () => leaveRoom())
    document.querySelector('#selectShareBtn').addEventListener('click', async () => await selectShareScreen(true))
    document.querySelector('#stopShareBtn').addEventListener('click', async () => await selectShareScreen(false))
    enabledAudioBtn.addEventListener('click', () => toggleAudioAction())
    enabledVideoBtn.addEventListener('click', () => toggleVideoAction())

    // 如果在录制过程中，切换了音视频流（普通<->分享屏幕），MediaRecorder 需要重新设置
    startRecordBtn.addEventListener('click', () => {
        if (isShare) {
            startRecord(shareStream)
        } else {
            startRecord(localStream)
        }
    })
    resumeRecordBtn.addEventListener('click', () => resumeRecord())
    pauseRecordBtn.addEventListener('click', () => pauseRecord())
    stopRecordBtn.addEventListener('click', () => stopRecord())
}

function handlePongMessage(json) {
    currentTimeoutCount = 0
}

function initHeartbeat(onclose) {
    currentTimeoutCount = 0
    let cancelable1 = setInterval(() => {
        const jsonMsg = {
            'eventCode': RTC_CMD_PING,
        }
        console.log(JSON.stringify(jsonMsg))
        ws.send(JSON.stringify(jsonMsg))
    }, heartbeatInterval)

    let cancelable2 = setInterval(() => {
        if (currentTimeoutCount > timeoutCount) {
            ws.close()
            onclose()
        }
        currentTimeoutCount += 1
    }, heartbeatInterval * 2)

    cancelable = [cancelable1, cancelable2]
}

function deinitHeartbeat() {
    cancelable.forEach((c) => clearInterval(c))
    cancelable = []
}

function newRecorder(stream) {
    const recorder = new MediaRecorder(stream, { mimeType: 'video/webm' })
    recorder.onstart = () => {
        recordStatus.innerHTML = '开始录制'
        srcUrl = null
    }
    recorder.onerror = (e) => {
        recordStatus.innerHTML = `录制错误 ${e}`
    }
    recorder.onpause = () => {
        recordStatus.innerHTML = '录制暂停'
    }
    recorder.onresume = () => {
        recordStatus.innerHTML = '继续录制， 录制中'
    }
    recorder.onstop = () => {
        recordStatus.innerHTML = '录制停止'
        let blob = new Blob(chunks, { type: 'video/webm' })
        chunks = []
        srcUrl = URL.createObjectURL(blob)

        saveRecord()
    }
    recorder.ondataavailable = (e) => {
        chunks.push(e.data)
    }
    return recorder
}

function startRecord(stream) {
    if (!mediaRecorder) {
        mediaRecorder = newRecorder(stream)
    }
    mediaRecorder.start()
    startRecordBtn.disabled = true
    resumeRecordBtn.disabled = true
    pauseRecordBtn.disabled = false
    stopRecordBtn.disabled = false

}

function resumeRecord() {
    mediaRecorder.resume()
    startRecordBtn.disabled = true
    resumeRecordBtn.disabled = true
    pauseRecordBtn.disabled = false
    stopRecordBtn.disabled = false
}

function pauseRecord() {
    mediaRecorder.pause()
    startRecordBtn.disabled = true
    resumeRecordBtn.disabled = false
    pauseRecordBtn.disabled = true
    stopRecordBtn.disabled = false
}

function stopRecord() {
    mediaRecorder.stop()
    startRecordBtn.disabled = false
    resumeRecordBtn.disabled = true
    pauseRecordBtn.disabled = true
    stopRecordBtn.disabled = true
    mediaRecorder = null
}

// video/webm 可以通过 ffmpeg 转换成 mp4
function saveRecord() {
    console.log(srcUrl)
    let a = document.createElement('a')
    document.body.appendChild(a)
    a.href = srcUrl
    a.download = '录屏'
    a.target = '_blank'
    a.click()
    document.body.removeChild(a)
}

init()