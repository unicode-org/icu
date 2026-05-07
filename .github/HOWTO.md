## How to create a Fedora docker image

For the general process and concepts see:
https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry

For our case I replaced the generic names with our own owner / repo / names / etc.

Run
```
docker login ghcr.io
```

When prompted use these:

* **User:** the github user
* **Password:** the github token

Update the timestamp (`20240929`) with the current date, ISO style:
```
docker build --tag ghcr.io/unicode-org/fedora-docker-gcr:20240929 -f Dockerfile_fedora .
docker push ghcr.io/unicode-org/fedora-docker-gcr:20240929
```

For more info see:
https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-docker-images

and:
https://stackoverflow.com/questions/64033686/how-can-i-use-private-docker-image-in-github-actions

To consider: generate and publish the docker image from a GitHub action.

---

The `DOCKER_CONTAINER_USER_NAME` and `DOCKER_CONTAINER_REGISTRY_TOKEN` used
in the action file for user and password are secrets already created.

They can be any GitHub user + token with the proper access rights.
Right now this is a token of the icu-robot account.
