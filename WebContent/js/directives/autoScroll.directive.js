class autoScroll {
    constructor () {
        this.scope = {
            autoScroll: "="
        };
    }
    link (scope, element, attr) {
        scope.$watchCollection('autoScroll', (newValue) =>{
            if (newValue && JSON.parse(attr.enableScroll)) {
                $(element).scrollTop($(element)[0].scrollHeight + $(element)[0].clientHeight);
            }
        });
    }
}
export default () => new autoScroll();