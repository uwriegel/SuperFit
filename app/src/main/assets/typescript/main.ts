
declare var Native: any

const starter = document.getElementsByClassName('starter')[0] as HTMLElement
const stopper = document.getElementsByClassName('stopper')[0] as HTMLElement

if (location.hash.substr(1) == "true")
    displayStop()
else
    displayStart()

const starterClicker = new ButtonClicker(starter, evt => starter, 
    typeof Native != undefined ? () => Native.doHapticFeedback() : null, 
    () => Native.start() )

const stopperClicker = new ButtonClicker(stopper, evt => stopper, 
    typeof Native != undefined ? () => Native.doHapticFeedback() : null, 
    () => Native.stop() )

function displayStart() {
    starter.classList.remove("hidden")
    stopper.classList.add("hidden")
}   

function displayStop() {
    stopper.classList.remove("hidden")
    starter.classList.add("hidden")
}   