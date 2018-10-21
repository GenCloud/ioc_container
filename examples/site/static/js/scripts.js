$(function () {
    $(".btn-info").click(function () {
        $(this).next().click();
    });

    $(".btn-auth").click(function () {
        var cooki = cookie();
        document.cookie = 'CSRF-TOKEN=' + cooki;

        $.ajax({
            url: "/enter",
            data: $('#auth_form').serialize(),
            headers: {'X-CSRF-TOKEN': cooki},
            crossDomain: true,
            xhrFields: {
                withCredentials: true
            },
            type: "POST",

            error: function () {
                new PNotify({
                    title: 'Error',
                    text: 'Error authentication',
                    type: 'error',
                    hide: false
                });
            },

            success: function () {
                new PNotify({
                    title: 'Success',
                    text: "Success authentication",
                    type: 'success',
                    hide: false
                });
            }
        })
    });

    $("input[type='file']").change(function () {
        $.ajax({
            type: "POST",
            url: "/upload",
            contentType: false,
            processData: false,
            data: new FormData($("form")[0]),
            cache: false,

            error: function () {
                new PNotify({
                    title: 'Error',
                    text: 'Unknown host error',
                    type: 'error',
                    hide: false
                });
            }
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
                        window.location.reload();
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

    $(".btn-warning").click(function () {
        $.ajax({
            type: "GET",
            url: "/remove",
            data: {name: this.name},

            error: function () {
                new PNotify({
                    title: 'Error',
                    text: 'Unknown host error',
                    type: 'error',
                    hide: false
                });
            }
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
                        window.location.reload();
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

    $(".btn-clear").click(function () {
        $.ajax({
            type: "GET",
            url: "/clear",

            error: function () {
                new PNotify({
                    title: 'Error',
                    text: 'Unknown host error',
                    type: 'error',
                    hide: false
                });
            }
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
                        window.location.reload();
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

    $(".btn-danger").click(function () {
        var data = $("#form").serialize();

        $.ajax({
            type: "POST",
            url: "/date",
            data: data
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