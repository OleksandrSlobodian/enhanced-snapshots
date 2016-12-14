export default function StAdvancedFilter () {

    var filterMatch = function (base, filter, type) {
        var typeOptions = {
            "str": function () {
                return filter.length == 0 || (base ? base.toLowerCase().indexOf(filter.toLowerCase()) > -1 : false)
            },
            "str-strict": function () {
                return filter.length == 0 || (base ? base === filter : false)
            },
            "int-range": function () {
                return (filter.lower <= base && filter.higher >= base)
            },
            "array-inc": function () {
                return filter.length == 0 || (base.length > 0 && (function () {
                        for (var i = 0; i < filter.length; i++) {
                            var tagToFilter = filter[i];
                            for (var j = 0; j < base.length; j++){
                                var tagFromBase = base[j];
                                if (tagFromBase.key === tagToFilter.key && tagFromBase.value === tagToFilter.value){
                                    return true;
                                }
                            }
                        }
                        return false;
                    })());
            }
        };
        return typeOptions[type]();
    };

    var volumeMatch = function (item, filterObj) {
        var filterKeys =  Object.keys(filterObj);
        for (var i = 0; i < filterKeys.length; i++){
            var key = filterKeys[i];
            if (!filterMatch(item[key], filterObj[key].value, filterObj[key].type)) {
                return false;
            }
        }

        return true;
    };

    return (array, filterObj) => {
        if (!angular.isUndefined(array)
            && !angular.isUndefined(filterObj)
            && array.length > 0) {
            var result = [];
            array.forEach(function (item) {
                if (volumeMatch(item, filterObj)){
                    result.push(item);
                }
            });

            return result;
        } else {
            return array;
        }
    };
}