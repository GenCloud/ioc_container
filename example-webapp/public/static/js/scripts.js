$(function () {
    $(".btn-create").click(function () {
        var cooki = cookie();
        document.cookie = 'CSRF-TOKEN=' + cooki;

        $.ajax({
            url: "/signup",
            data: $('#creation').serialize(),
            headers: {'X-CSRF-TOKEN': cooki},
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            type: "POST"
        }).done(function (data) {
            switch (data.type) {
                case 'OK':
                    new PNotify({
                        title: 'Success',
                        text: data.message,
                        type: 'success',
                        hide: false
                    });
                    break;
                case 'ERROR':
                    new PNotify({
                        title: 'Error',
                        text: data.message,
                        type: 'error',
                        hide: false
                    });
                    break;
            }
        });
    });

    $(".btn-auth").click(function () {
        var cooki = cookie();
        document.cookie = 'CSRF-TOKEN=' + cooki;

        $.ajax({
            url: "/signin",
            data: $('#auth').serialize(),
            headers: {'X-CSRF-TOKEN': cooki},
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            type: "POST"

        }).done(function (data) {
            switch (data.type) {
                case 'OK':
                    new PNotify({
                        title: 'Success',
                        text: data.message,
                        type: 'success',
                        hide: false
                    });

                    setTimeout(function () {
                        window.location = "/loginPage";
                    }, 5000);
                    break;
                case 'ERROR':
                    new PNotify({
                        title: 'Error',
                        text: data.message,
                        type: 'error',
                        hide: false
                    });
                    break;
            }
        });
    });

    $(".btn-logout").click(function () {
        $.ajax({
            url: "/signout",
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            type: "GET"
        }).done(function (data) {
            switch (data.type) {
                case 'OK':
                    new PNotify({
                        title: 'Success',
                        text: 'Logouting...',
                        type: 'success',
                        hide: false
                    });

                    setTimeout(function () {
                        window.location = data.message;
                    }, 5000);
                    break;
                case 'ERROR':
                    new PNotify({
                        title: 'Error',
                        text: data.message,
                        type: 'error',
                        hide: false
                    });
                    break;
            }
        });
    });
});

function cookie(a) {
    return a           // if the placeholder was passed, return
        ? (              // a random number from 0 to 15
            a ^            // unless b is 8,
            Math.random()  // in which case
            * 16           // a random number from
            >> a / 4         // 8 to 11
        ).toString(16) // in hexadecimal
        : (              // or otherwise a concatenated string:
            [1e7] +        // 10000000 +
            -1e3 +         // -1000 +
            -4e3 +         // -4000 +
            -8e3 +         // -80000000 +
            -1e11          // -100000000000,
        ).replace(     // replacing
            /[018]/g,    // zeroes, ones, and eights with
            cookie           // random hex digits
        )
}