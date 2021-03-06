﻿

var Connection = (function () {
    async function checkConnection() {
        try {
            await invoke("check")
            return true
        } catch {
            return false
        }
    }

    async function stop() {
        try {
            return await invoke("stop")
        } catch{}
    }

    function invoke<T>(method: string, param?: any) {
        return new Promise<T>((resolve, reject) => {
            var xmlhttp = new XMLHttpRequest()
            xmlhttp.onload = evt => {
                if (xmlhttp.readyState == 4 && xmlhttp.status == 200) {
                    var result = xmlhttp.response
                    resolve(result)
                }
                else
                    reject("")
            }
            xmlhttp.onerror = e => reject(e)
            xmlhttp.open('GET', `http://localhost:9865/${method}`, true)
            xmlhttp.responseType = 'json'
            xmlhttp.send(JSON.stringify(param))
        })
    }

    return {
        checkConnection: checkConnection,
        stop: stop
    }
})()