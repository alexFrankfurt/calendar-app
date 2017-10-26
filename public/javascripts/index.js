body = document.querySelector("body")
redir = false

function onSignIn(googleUser) {
    const id_token = googleUser.getAuthResponse().id_token;
    const profile = googleUser.getBasicProfile();
    console.log('ID: ' + profile.getId()); // Do not send to your backend! Use an ID token instead.
    console.log('Name: ' + profile.getName());
    console.log('Image URL: ' + profile.getImageUrl());
    console.log('Email: ' + profile.getEmail());

    const initObj = {
        method: 'POST',
        redirect: 'follow',
        headers: {"Content-Type": "application/x-www-form-urlencoded"},
        body: 'idtoken=' + id_token,
        credentials: 'include'
    }

    fetch("/tokensignin", initObj).then(resp => {
        return resp.text()
    }).then(txt => {
        console.log("Signed in as: " + txt)
        window.location = txt
    })
}
