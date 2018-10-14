$(function () {
    $(".btn-info").click(function () {
        $(this).next().click();
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

    $(".btn-success").click(function () {
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
            type: "GET",
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