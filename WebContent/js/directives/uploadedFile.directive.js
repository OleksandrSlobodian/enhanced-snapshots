class UploadedFile {
    constructor () {
        this.scope = {
            'uploadedFile': '='
        };
    }
    link (scope, el, attrs){
        el.bind('change', function(event){
            var file = event.target.files[0];
            scope.uploadedFile = file ? file : undefined;
            scope.$apply();
        });
    }
}

export default () => new UploadedFile();