# Deploy to Google Cloud Run

## Prerequisites
- [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) installed
- Docker installed
- A Google Cloud project with billing enabled

---

## Step 1: Set your environment variables

```bash
export PROJECT_ID=your-gcp-project-id
export REGION=us-central1
export SERVICE_NAME=daily-tech-news-agent
export LLM_API_KEY=your-groq-api-key   # Get free at https://console.groq.com
```

---

## Step 2: Authenticate & configure GCP

```bash
gcloud auth login
gcloud config set project $PROJECT_ID

# Enable required APIs
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com
```

---

## Step 3: Build & push Docker image

```bash
# Create Artifact Registry repo
gcloud artifacts repositories create $SERVICE_NAME \
  --repository-format=docker \
  --location=$REGION

# Configure Docker auth
gcloud auth configure-docker $REGION-docker.pkg.dev

# Build and push
IMAGE="$REGION-docker.pkg.dev/$PROJECT_ID/$SERVICE_NAME/$SERVICE_NAME:latest"
docker build -t $IMAGE .
docker push $IMAGE
```

---

## Step 4: Deploy to Cloud Run

```bash
gcloud run deploy $SERVICE_NAME \
  --image=$IMAGE \
  --platform=managed \
  --region=$REGION \
  --allow-unauthenticated \
  --port=8080 \
  --memory=1Gi \
  --cpu=1 \
  --min-instances=0 \
  --max-instances=3 \
  --set-env-vars="SPRING_PROFILES_ACTIVE=prod,LLM_PROVIDER=groq,LLM_MODEL=llama-3.1-8b-instant" \
  --set-env-vars="LLM_API_KEY=$LLM_API_KEY"
```

Cloud Run will print the **Service URL** when done. Open it in your browser!

---

## Step 5: Verify

```bash
SERVICE_URL=$(gcloud run services describe $SERVICE_NAME --region=$REGION --format='value(status.url)')
curl $SERVICE_URL/health
```

---

## Update (re-deploy after code changes)

```bash
docker build -t $IMAGE .
docker push $IMAGE
gcloud run deploy $SERVICE_NAME --image=$IMAGE --region=$REGION
```

---

## Useful commands

```bash
# View logs
gcloud logging read "resource.type=cloud_run_revision AND resource.labels.service_name=$SERVICE_NAME" --limit=50

# Scale to zero (stop billing when idle)
gcloud run services update $SERVICE_NAME --min-instances=0 --region=$REGION

# Delete service
gcloud run services delete $SERVICE_NAME --region=$REGION
```

---

## Cost estimate
Cloud Run with `--min-instances=0` costs **$0 when idle** — you only pay for actual requests.
Free tier: 2 million requests/month, 360,000 GB-seconds/month.
