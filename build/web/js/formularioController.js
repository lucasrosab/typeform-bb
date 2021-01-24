app.controller('formularioController', function ($scope, $http) {

    $scope.perguntas = [];
    $scope.modeloRelacionamento =[
        {idRelacionamento: 1, nome: 'Estilo'},
        {idRelacionamento: 2, nome: 'Estilo Agro'},
        {idRelacionamento: 3, nome: 'Exclusivo'},
        {idRelacionamento: 4, nome: 'Personalizado'},
        {idRelacionamento: 5, nome: 'Personalizado Agro'},
        {idRelacionamento: 6, nome: 'Potencial PF'},
        {idRelacionamento: 7, nome: 'Varejo I'},
        {idRelacionamento: 8, nome: 'Varejo II'}
    ];

    /*Trazendo as Perguntas */
    function carregarPerguntas() {
        $http({
            method: "GET",
            url: "dados",
            params: "perguntas"
        }).then(function mySuccess(response) {
            console.log("entrou no success");
            $scope.perguntas = response.data;
            console.log("dados obtidos: " + $scope.perguntas);
        }, function myError(response) {
            console.log("entrou error");
            $scope.perguntas = response.statusText;
            console.log("dados obtidos: " + $scope.perguntas);
        });
    }

    carregarPerguntas();
    
    
        
    

    $scope.salvar = function (respostas) {
        console.log(respostas);
        $http({
            method: "POST",
            url: "typeform",
            params: "respostas"
        }).then(function mySuccess(response) {
            console.log("entrou no success");
            $scope.perguntas = response.data;
            console.log("dados obtidos: " + $scope.perguntas);
        }, function myError(response) {
            console.log("entrou error");
            $scope.perguntas = response.statusText;
            console.log("dados obtidos: " + $scope.perguntas);
        });
        
        
        
        
    };

    $scope.cancelar = function () {
        $scope.respostas = {};
    };
});

