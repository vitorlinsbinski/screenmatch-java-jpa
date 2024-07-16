package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();
    private SerieRepository repositorio;
    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    public void exibeMenu() {
        var opcao = -1;

        while(opcao != 0) {
            var menu = """
                1 - Buscar séries
                2 - Buscar episódios
                3 - Listar séries buscadas
                4 - Buscar série por título
                5 - Buscar séries por ator
                6 - Top 5 séries
                7 - Buscar por categoria
                8 - Buscar por temporadas e avaliação
                
                0 - Sair                                 
                """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriesPorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarPorCategoria();
                    break;
                case 8:
                    buscarPorTemporadasEAvaliacao();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }

    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        System.out.println(dados);

        this.repositorio.save(serie);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();

        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = this.leitura.nextLine();

        Optional<Serie> serie =
                this.repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serie.isPresent()) {
            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }

            temporadas.forEach(System.out::println);

            List<Episodio> novosEpisodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .toList();

            List<Episodio> episodiosExistentes = serieEncontrada.getEpisodios() != null ?
                    serieEncontrada.getEpisodios() :
                    new ArrayList<>();

            Map<String, Episodio> episodiosMap = episodiosExistentes.stream()
                    .collect(Collectors.toMap(Episodio::getIdentificadorUnico, e -> e)); // Usando o identificador único

            novosEpisodios.forEach(novoEpisodio -> {
                Episodio episodioExistente = episodiosMap.get(novoEpisodio.getIdentificadorUnico());

                if (episodioExistente == null || !episodioExistente.equals(novoEpisodio)) {
                    episodiosMap.put(novoEpisodio.getIdentificadorUnico(), novoEpisodio);
                }
            });

            List<Episodio> episodiosAtualizados = new ArrayList<>(episodiosMap.values());
            serieEncontrada.setEpisodios(episodiosAtualizados);

            repositorio.save(serieEncontrada);

            System.out.println("Episódios atualizados:");
            episodiosAtualizados.forEach(System.out::println);
        } else {
            System.out.println("Série não encontrada");
        }
    }


    private void listarSeriesBuscadas() {
        this.series = this.repositorio.findAll();

        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma série pelo nome: ");
        var nomeSerie = this.leitura.nextLine();

        Optional<Serie> serieBuscada =
                this.repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBuscada.isPresent()) {
            System.out.println("Dados da série: " + serieBuscada.get());
        } else {
            System.out.println("Série não encontrada");
        }
    }

    private void buscarSeriesPorAtor() {
        System.out.println("Qual o nome para busca? ");
        var nomeAtor = this.leitura.nextLine();

        System.out.println("Avaliações a partir de qual valor? ");
        var avaliacao = this.leitura.nextDouble();

        List<Serie> seriesEncontradas =
                this.repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avaliacao);

        seriesEncontradas.forEach(s -> {
            System.out.println(s.getTitulo() + ", avaliação: " + s.getAvaliacao());
        });
    }

    private void buscarTop5Series() {
        List<Serie> seriesTop = this.repositorio.findTop5ByOrderByAvaliacaoDesc();

        seriesTop.forEach(s -> {
            System.out.println(s.getTitulo() + ", avaliação: " + s.getAvaliacao());
        });
    }

    private void buscarPorCategoria() {
        System.out.println("Deseja buscar séries de que categoria/gênero?");
        var nomeGenero = this.leitura.nextLine();

        Categoria categoria = Categoria.fromPortugues(nomeGenero);

        List<Serie> seriesPorCategoria =
                this.repositorio.findByGenero(categoria);

        System.out.println("Séries da categoria: " + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void buscarPorTemporadasEAvaliacao() {
        System.out.println("Digite o número máximo de temporadas");
        var totalTemporadas = this.leitura.nextInt();

        System.out.println("Avaliações a partir de qual valor? ");
        var avaliacao = this.leitura.nextDouble();

        List<Serie> seriesPorTemporadasEAvaliacao = this.repositorio
                .findByTotalTemporadasLessThanEqualAndAvaliacaoGreaterThanEqual(totalTemporadas, avaliacao);

        seriesPorTemporadasEAvaliacao.forEach(System.out::println);
    }

}