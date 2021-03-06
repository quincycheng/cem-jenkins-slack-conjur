pipeline {
    agent any
    
    stages {
        stage('Get Event Type') {
            steps {
                echo "Event Type: $event_type"
            }
        }
        stage('Workspace connection error') {
            when {
                environment name: 'event_type', value: 'accountConnectionError' 
            }
            steps {
                slackSend (
                    color: "danger",
                    message: "🔥 *" + event_payload_account_name + " on " + event_payload_platform_name + " connection error*\n"
                        + "      🏥 " + event_payload_status + "\n"
                        + "      🩺 " + event_payload_status_reason + "\n"
                        + "      ⏰ " +  event_payload_scan_date +"\n"
                        + "      💊 Login to <https://cem.cyberark.com|CyberArk CEM> to fix it"
                )
            }
        }
        stage('Total Exposure increased') {
            when {
                environment name: 'event_type', value: 'globalExposureLevelExceeded' 
            }
            steps {
                slackSend (
                    color: "warning",
                    message: "🆙 *Total Exposure Increased*\n"
                        + "      🔺 " +  event_payload_new_exposure_level 
                        + " (was: " +  event_payload_previous_exposure_level +")\n"
                        + "      ⏰ " +  event_payload_scan_date +"\n"
                )
            }
            
        }
        stage('Entities Exposure Level Increased') {
            when {
                environment name: 'event_type', value: 'entitiesExposureLevelIncreased' 
            }
            steps {
                script {
                    def payloadObj = readJSON text: env.event_payload
                    
                    sh ("echo '"+ env.event_payload +"' | jq . ")

                }
            }
        }
    }
}
