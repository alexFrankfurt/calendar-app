
const contentDiv = document.querySelector(".content")
const personsButton = document.querySelector("#persons-tab")
const addPersonButton = document.querySelector("#add-person-tab")
const vacationsButton = document.querySelector("#vacations-tab")
const addVacationButton = document.querySelector("#add-vacation-tab")
const myVacationsButton = document.querySelector("#my-vacations-tab")

function validVacationRange(end, st) {
    if (end - st > 172800000 && end - st < 1296000000) return true
    else return false
}

personsButton.onclick = () => {
    fetch("/panel/persons", {credentials: 'include'}).then((resp) => resp.text()).then( (data) => {
            contentDiv.innerHTML = data
            personsPanel()
        }
    )
}

addPersonButton.onclick = () => {
    fetch("/panel/person/add", {credentials: 'include'}).then((resp) => resp.text()).then( (data) => {
        contentDiv.innerHTML = data
        addPersonPanel()
    })
}

vacationsButton.onclick = () => {
    fetch("/panel/vacations", {credentials: 'include'}).then(resp => resp.text()).then( (data) => {
        contentDiv.innerHTML = data
        vacationsPanel()
    })
}

addVacationButton.onclick = () => {
    fetch("/panel/vacation/add", {credentials: 'include'}).then((resp) => resp.text()).then( (data) => {
        contentDiv.innerHTML = data
        addVacationPanel()
    })
}

function onLoad() {
    gapi.load('auth2', function() {
        gapi.auth2.init();
    });
}

function signOut() {
    var auth2 = gapi.auth2.getAuthInstance();
    auth2.signOut().then(function () {
        console.log('User signed out.');
    });
    fetch("/signout", {method: "POST"})
    window.location = "/"
}

function personsPanel() {
    const frm = document.querySelector(".content>form")
    const pData = document.querySelector("#persons-data")

    frm.onsubmit = (e) => {
        e.preventDefault()
        const fd = new FormData(frm)
        let order = fd.get("order");
        console.log(order)
        let pos = fd.get("position");
        console.log(pos)

        const req = (qStr) => {
            fetch(qStr, {credentials: 'include'}).then(resp => resp.text()).then(data =>
                pData.innerHTML = data
            )
        }

        if( pos == "") {
            req(`/persons?order=${order}`)
        } else {
            req(`/persons?order=${order}&position=${pos}`)
        }
    }
}

function addPersonPanel() {
    const frm = document.querySelector(".content>form")
    frm.onsubmit = (e) => {
        e.preventDefault()

        const fd = new FormData(frm)
        fetch("/persons?notme=true", {
            method: 'POST',
            credentials: 'include',
            body: fd
        }).then(resp => {
            if (resp.redirected == true) contentDiv.innerText = "Successful addition"
            else contentDiv.innerText = "Failure adding"
        })
    }
}

function vacationsPanel() {
    const frm = document.querySelector(".content>form")
    const vd = document.querySelector("#vacations-data")
    frm.onsubmit = (e) => {
        e.preventDefault()
        const fd = new FormData(frm)
        let order = fd.get("order");
        console.log(order)
        let from = (new Date(fd.get("from"))).getTime()
        console.log(from)
        if (isNaN(from)) from = 0
        let to   = (new Date(fd.get("to"))).getTime()
        if (isNaN(to)) to = 10033030300300
        console.log(to)

        fetch(`/vacations?order=${order}&from=${from}&to=${to}`, {credentials: 'include'}).then(resp => resp.text())
            .then( txt => {
                vd.innerHTML = txt
            })
    }
}

function editVac(but) {
    const id = but.value
    const html = `<input type="date" name="startDate" id="start-date">
                  <input type="date" name="endDate" id="end-date">
                  <div id="inf"></div>
                  <button id="update-button">Update</button> `
    contentDiv.innerHTML = html
    const sd = document.querySelector("#start-date")
    const ed = document.querySelector("#end-date")
    const inf = document.querySelector("#inf")
    document.querySelector("#update-button").onclick = () => {
        inf.innerText = ""
        console.log("sd: " + sd.value)
        console.log("ed: " + ed.value)
        if (sd.value == "" || ed.value == "") {}
        else if (!validVacationRange(new Date(ed.value).getTime(), new Date(sd.value).getTime())) {
          inf.innerText = "Vacation range should be between 2 and 15 days."
        } else {
            let from = new Date(sd.value).getTime()
            let to = new Date(ed.value).getTime()
            fetch(`/vacation/${id}?from=${from}&to=${to}`, {credentials: 'include', method: 'PATCH'}).then((resp) =>
                resp.text()
            ).then( data => {
                console.log("Updated: " + data)
                contentDiv.innerText = "Successfully updated."
            })
        }
    }
}

function delVac(but) {
    fetch(`/vacations/${but.value}`, {credentials: 'include', method: 'DELETE'}).then(resp => resp.text()).then(data => {
        console.log("Deleted: " + data)
    })
}

function addVacationPanel() {
    const frm = document.querySelector(".content>form")
    frm.onsubmit = (e) => {
        e.preventDefault()

        const fd = new FormData(frm)
        const inf = document.querySelector("#inf")
        const sd = fd.get("startDate")
        const ed = fd.get("endDate")
        const id = fd.get("uid")
        console.log(sd + ed + id)
        if (sd == "" || ed == "" || id == "") inf.innerText = "Not full form"
        else if (!validVacationRange(new Date(ed).getTime(), new Date(sd).getTime())) {
            inf.innerText = "Vacation range should be between 2 and 15 days."
        } else {
            fetch("/vacations", {
                method: 'POST',
                credentials: 'include',
                body: fd
            }).then(resp => {
                if (resp.ok) contentDiv.innerText = "Successful addition"
                else contentDiv.innerText = "Failure adding"
                return resp.text()
            }).then(txt => contentDiv.innerText += txt)
        }

    }
}