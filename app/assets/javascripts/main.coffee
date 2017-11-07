contentDiv = document.querySelector('.content')
personsButton = document.querySelector('#persons-tab')
addPersonButton = document.querySelector('#add-person-tab')
vacationsButton = document.querySelector('#vacations-tab')
addVacationButton = document.querySelector('#add-vacation-tab')
myVacationsButton = document.querySelector('#my-vacations-tab')

validVacationRange = (end, st) ->
  if end - st > 172800000 and end - st < 1296000000 then true
  else false

personsButton.onclick = ->
  fetch('/panel/persons', credentials: 'include').then (resp) -> resp.text()
    .then (data) ->
      contentDiv.innerHTML = data
      personsPanel()


addPersonButton.onclick = ->
  fetch("/panel/person/add", {credentials: 'include'}).then((resp) -> resp.text()).then (data) ->
    contentDiv.innerHTML = data
    addPersonPanel()


vacationsButton.onclick = ->
  fetch("/panel/vacations", {credentials: 'include'}).then((resp) -> resp.text()).then (data) ->
    contentDiv.innerHTML = data
    vacationsPanel()


addVacationButton.onclick = ->
  fetch("/panel/vacation/add", {credentials: 'include'}).then((resp) -> resp.text()).then (data) ->
    contentDiv.innerHTML = data
    addVacationPanel()

onLoad = ->
  gapi.load 'auth2', -> gapi.auth2.init()

signOut = ->
  auth2 = gapi.auth2.getAuthInstance()
  auth2.signOut().then ->
    console.log 'User signed out.'
  fetch "/signout", method: "POST"
  window.location = "/"

personsPanel = ->
    frm = document.querySelector(".content>form")
    pData = document.querySelector("#persons-data")

    frm.onsubmit = (e) ->
      e.preventDefault()
      fd = new FormData(frm)
      order = fd.get("order")
      console.log(order)
      pos = fd.get("position");
      console.log(pos)

      req = (qStr) ->
        fetch(qStr, {credentials: 'include'}).then((resp) -> resp.text()).then (data) ->
          pData.innerHTML = data

      if pos == ""
        req "/persons?order=#{order}"
      else
        req "/persons?order=#{order}&position=#{pos}"

addPersonPanel = ->
    frm = document.querySelector(".content>form")
    frm.onsubmit = (e) ->
      e.preventDefault()
      fd = new FormData(frm)
      fetch "/persons?notme=true",
          method: 'POST',
          credentials: 'include',
          body: fd
        .then (resp) ->
          if resp.redirected == true
            contentDiv.innerText = "Successful addition"
          else contentDiv.innerText = "Failure adding"

vacationsPanel = ->
    frm = document.querySelector(".content>form")
    vd = document.querySelector("#vacations-data")
    frm.onsubmit = (e) ->
      e.preventDefault()
      fd = new FormData(frm)
      order = fd.get("order");
      console.log(order)
      from = (new Date(fd.get("from"))).getTime()
      console.log(from)
      if (isNaN(from))
        from = 0
        to = (new Date(fd.get("to"))).getTime()
      if (isNaN(to))
        to = 10033030300300
        console.log(to)
      fetch("/vacations?order=#{order}&from=#{from}&to=#{to}", {credentials: 'include'}).then((resp) -> resp.text())
        .then (txt) ->
          vd.innerHTML = txt

editVac = (but) ->
    id = but.value
    html = """<input type="date" name="startDate" id="start-date">
              <input type="date" name="endDate" id="end-date">
              <div id="inf"></div>
              <button id="update-button">Update</button> """
    contentDiv.innerHTML = html
    sd = document.querySelector("#start-date")
    ed = document.querySelector("#end-date")
    inf = document.querySelector("#inf")
    document.querySelector("#update-button").onclick = ->
      inf.innerText = ""
    console.log("sd: " + sd.value)
    console.log("ed: " + ed.value)
    if (sd.value == "" || ed.value == "") {}
    else if !validVacationRange(new Date(ed.value).getTime(), new Date(sd.value).getTime())
      inf.innerText = "Vacation range should be between 2 and 15 days."
    else
      from = new Date(sd.value).getTime()
      to = new Date(ed.value).getTime()
      fetch "/vacation/#{id}?from=#{from}&to=#{to}", {credentials: 'include', method: 'PATCH'}
        .then (resp) -> resp.text()
        .then (data) ->
          console.log("Updated: " + data)
          contentDiv.innerText = "Successfully updated."

delVac = (but) ->
  fetch("/vacations/#{but.value}", {credentials: 'include', method: 'DELETE'}).then(resp -> resp.text()).then (data) ->
    console.log("Deleted: " + data)

addVacationPanel = ->
    frm = document.querySelector(".content>form")
    frm.onsubmit = (e) ->
      e.preventDefault()
      fd = new FormData(frm)
      inf = document.querySelector("#inf")
      sd = fd.get("startDate")
      ed = fd.get("endDate")
      id = fd.get("uid")
      console.log(sd + ed + id)
      if (sd == "" || ed == "" || id == "")
        inf.innerText = "Not full form"
      else if !validVacationRange(new Date(ed).getTime(), new Date(sd).getTime())
        inf.innerText = "Vacation range should be between 2 and 15 days."
      else
        fetch "/vacations",
            method: 'POST',
            credentials: 'include',
            body: fd
          .then (resp) ->
            if (resp.ok)
              contentDiv.innerText = "Successful addition"
            else contentDiv.innerText = "Failure adding"
            resp.text()
          .then((txt) -> contentDiv.innerText += txt)