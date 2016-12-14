class TagFilter {
    constructor () {
        this.scope = {
            tags: '=',
            src: '=',
            keyph: '@',
            valueph: '@'
        };
        this.restrict = 'E';
        this.template =
            '<div class="input-group tag-input" style="clear: both;">' +
            '<input type="text" class="form-control" ng-model="newTag.key" placeholder="{{keyph}}" typeahead="key for key in srcKeys | filter:$viewValue" typeahead-editable="false"/>' +
            '<span class="input-group-btn" style="width:0px;"></span>' +
            '<input type="text" class="form-control" ng-model="newTag.value" placeholder="{{valueph}}" style="border-left: 0" typeahead="val for val in src[newTag.key] | filter:$viewValue" typeahead-editable="false" />' +
            '<span class="input-group-btn"><button class="btn btn-primary" ng-click="add()"><span class="glyphicon glyphicon-plus"></span></button></span>' +
            '</div>' +
            '<div class="tags">' +
            '<div ng-repeat="tag in tags track by $index" class="tag label label-success" ng-click="remove($index)">' +
            '<span class="glyphicon glyphicon-remove"></span>' +
            '<div class="tag-value">{{tag.key}} : {{tag.value}}</div>' +
            '</div>' +
            '</div>';
    }
    link ($scope, $element, $attrs ) {
        $scope.newTag = {};
        $scope.srcKeys = [];
        var inputs = angular.element( $element[0].querySelectorAll('input') );

        $scope.$watch("src", function (v) {
            $scope.srcKeys = Object.keys(v) || [];
        });

        // This adds the new tag to the tags array
        $scope.add = function() {
            if ($scope.newTag.hasOwnProperty('key') && $scope.newTag.hasOwnProperty('value')) {
                $scope.tags.push($scope.newTag);
                $scope.newTag = {};
            }
            event.preventDefault();
        };

        // This is the ng-click handler to remove an item
        $scope.remove = function ( idx ) {
            $scope.tags.splice( idx , 1 );
        };

        // Capture all keypresses
        inputs.bind( 'keypress', function ( event ) {
            // But we only care when Enter was pressed
            if ( $scope.newTag.key != "" &&  $scope.newTag.value != "" && ( event.keyCode == 13 ) ) {
                event.preventDefault();
                $scope.$apply( $scope.add );
            }
        });
    }
}

export default () => new TagFilter();