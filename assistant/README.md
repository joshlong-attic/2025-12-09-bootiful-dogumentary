## Deploy to Cloud Foundry

```bash
./mvnw -Pcloudfoundry clean package
cf push -f ./manifest.yml
```

This vectorizes the ```dog``` table when its started, so it's expecting the database to be already populated with data.