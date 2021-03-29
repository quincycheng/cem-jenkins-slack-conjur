# WORK IN PROGRESS

# SecOps + ChatOps = No more Click-Ops

## Overview 

### The Pipeline

![secops](https://user-images.githubusercontent.com/4685314/112829586-e6cab980-90c3-11eb-8299-f543c7b65124.png)

### UX

![preview](https://user-images.githubusercontent.com/4685314/112829622-f5b16c00-90c3-11eb-8c6d-502e8a0ab23f.gif)


## Technical Info

### Jenkins
 - new alert pipeline: [jenkins/cem-new-alert/](./jenkins/cem-new-alert/) folder
 - apply remediations pipeline: [jenkins/cem-apply-fixes/](./jenkins/cem-apply-fixes/) folder

### Slack
- Java
- AWS Lambda
- Build & Deploy:

```
export CACHE_VIEW=24 # hours
export CONJUR_ACCOUNT=default # Change it
export CONJUR_APPLIANCE_URL=https://<your conjur server>
export CONJUR_AUTHN_LOGIN=host/SecOps/<AWS account>/<AWS IAM role>
export CONJUR_AUTHN_SERVICE_ID=<AWS IAM Authn name>
export CEM_PASS=<Path to CEM API Key variable in Conjur>
export CEM_ORG=<Path to CEM Organization variable in Conjur>
export JENKINS_URL=https://<Your Jenkins Server>job/cem-apply-fixes/buildWithParameters
export JENKINS_USER=<Path to Jenkins User Name variable in Conjur>
export JENKINS_PASS=<Path to Jenkins Password variable in Conjur>

export SLACK_SIGNING_SECRET=<Slack Signing Secret>
export SLACK_BOT_TOKEN=<Slack Bot Token>

cd cem-slack-java
./gradlew build && \
./gradlew clean build && \
npx serverless deploy && \
npx serverless invoke --function warmup
cd ..
```

### Conjur
Policies: [conjur](./conjur) folder


