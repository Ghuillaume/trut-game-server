# Trut Game Server


## Deployment

gcloud run deploy trut-game-server \
     --source . \
     --region europe-west1 \
     --allow-unauthenticated \
     --port 8080 \
     --session-affinity \
     --min-instances 0 --max-instances 2
