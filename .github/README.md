## How to create a Fedora docker image

Run
```
docker login ghcr.io
```

When prompted use these:

* **User:** the github user
* **Password:** the github key

Update the timestamp (`20240929`) with the current date, ISO style:
```
docker build --tag ghcr.io/unicode-org/fedora-docker-gcr:20240929 -f Dockerfile_fedora .
docker push ghcr.io/unicode-org/fedora-docker-gcr:20240929
```

See:
https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-docker-images

Also:
https://stackoverflow.com/questions/64033686/how-can-i-use-private-docker-image-in-github-actions

To consider: generate and publish the docker image from a GitHub action.
