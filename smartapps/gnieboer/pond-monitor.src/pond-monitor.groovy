/**
 *  Open a valve and turn off a switch if moisture is no longer detected
 *  Close the valve and turn the switch on if moisture is detected
 *
 *  Copyright 2017 GCN Development
 *
 *	Author: Geof Nieboer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
definition(
    name: "Pond Monitor",
    namespace: "gnieboer",
    author: "Geof Nieboer",
    description: "Open a (fill) valve and turn off a (pump) switch if pond level drops (moisture not detected).  Reverse the process once pond is full",
    category: "Safety & Security",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/dry-the-wet-spot@2x.png"
)

preferences {
	section("Pond level sensor") {
		input "sensor", "capability.waterSensor", title: "Water Sensor", required: true, multiple: false
	}
	section("Fill Valve") {
		input "valve", "capability.valve", title: "Fill Valve", required: false, multiple: false
	}
    section("Pond Pump") {
		input "pump", "capability.switch", title: "Pump", required: false, multiple: false
	}
	section("Send this message (optional, sends standard status message if not specified)"){
		input "messageText", "text", title: "Message Text", required: false
	}
	section("Via a push notification and/or an SMS message"){
		input "phone", "phone", title: "Phone Number (for SMS, optional)", required: false
		input "pushAndPhone", "enum", title: "Both Push and SMS?", required: false, options: ["Yes","No"]
	}
	section("Minimum time between messages (optional)") {
		input "frequency", "decimal", title: "Minutes", required: false
	}    
}

def installed() {
 	subscribe(sensor, "water", waterHandler)
}

def updated() {
	unsubscribe()
 	subscribe(sensor, "water", waterHandler)
}

def waterHandler(evt) {
	log.debug "Sensor says ${evt.value}"
	if (evt.value == "wet") {
		valve.close()
        pump.on()
	} else 
    {
        valve.open()
        pump.off()
       	if (frequency) {
			def lastTime = state[evt.deviceId]
			if (lastTime == null || now() - lastTime >= frequency * 60000) {
				sendMessage(evt)
			}
		}
		else {
			sendMessage(evt)
		}   
    }
}

private sendMessage(evt) {
	def msg = messageText ?: "Pond level low warning"
	log.debug "$evt.name:$evt.value, pushAndPhone:$pushAndPhone, '$msg'"

	if (!phone || pushAndPhone != "No") {
		log.debug "sending push"
		sendPush(msg)
	}
	if (phone) {
		log.debug "sending SMS"
		sendSms(phone, msg)
	}
	if (frequency) {
		state[evt.deviceId] = now()
	}
}