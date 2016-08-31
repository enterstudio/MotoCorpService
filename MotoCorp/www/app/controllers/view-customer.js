app.controller('ViewCustomerCtrl', function ($scope, $state, CRM) {
  var id = $state.params.id;
  

  $scope.addVisit = function () {
    $state.go('app.new-visit', {id: id});
  };

  CRM.getCustomer(id).then(function (customer) {
    $scope.customer = customer;
    $scope.$apply();
  }).fail(function (error) {
    alert(error);
  });

  /* Create a new customer with inputs */
  $scope.newCustomer = function(){
    CRM.newCustomer({name: this.name, plate: this.plate, make: this.make, model: this.model, vin: this.vin}).then(function (results) {
      if(results.length === 0) {
        alert('No match found');
      } else {
        $scope.newCustomer = results;
        $scope.$apply();
      }
    }).fail(function (error) {
      alert(error);
    });
  };

});