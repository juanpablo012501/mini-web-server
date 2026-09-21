// ── Utilidades ──

function showLoading(id) {
    document.getElementById(id + 'Loading').classList.remove('hidden');
    document.getElementById(id + 'Result').classList.add('hidden');
    document.getElementById(id + 'Error').classList.add('hidden');
}

function showResult(id, message) {
    document.getElementById(id + 'Loading').classList.add('hidden');
    document.getElementById(id + 'Result').classList.remove('hidden');
    document.getElementById(id + 'Result').innerHTML = message;
    document.getElementById(id + 'Error').classList.add('hidden');
}

function showError(id, message) {
    document.getElementById(id + 'Loading').classList.add('hidden');
    document.getElementById(id + 'Result').classList.add('hidden');
    document.getElementById(id + 'Error').classList.remove('hidden');
    document.getElementById(id + 'Error').innerHTML = message;
}

// ── Saludo ──

function greet() {
    const name = document.getElementById('greetName').value.trim();

    if (!name) {
        showError('greet', 'Por favor ingresa un nombre.');
        return;
    }

    showLoading('greet');

    fetch('/api/greeting?name=' + encodeURIComponent(name))
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(data => {
            showResult('greet', ':) ' + data.greeting);
        })
        .catch(error => {
            showError('greet', 'ERROR: ' + error.message);
        });
}

// ── Cuadrado ──

function square() {
    const value = document.getElementById('squareValue').value.trim();

    if (!value) {
        showError('square', 'Por favor ingresa un número.');
        return;
    }

    showLoading('square');

    fetch('/api/square?value=' + encodeURIComponent(value))
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(data => {
            showResult('square',  '' + data.input + '² = ' + data.square);
        })
        .catch(error => {
            showError('square', 'ERROR: '  + error.message);
        });
}

// ── Hora del servidor ──

function getTime() {
    showLoading('time');

    fetch('/api/time')
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(data => {
            showResult('time', '' + data.serverTime);
        })
        .catch(error => {
            showError('time', 'ERROR: ' + error.message);
        });
}

// ── Health ──

function checkHealth() {
    showLoading('health');

    fetch('/api/health')
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => { throw new Error(text); });
            }
            return response.json();
        })
        .then(data => {
            showResult('health', 'ESTADO: ' + data.status);
        })
        .catch(error => {
            showError('health', 'ERROR: ' + error.message);
        });
}