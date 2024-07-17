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
    private Optional<Serie> serieBusca;

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
                9 - Buscar episódio por trecho
                10 - Top 5 episódios por série
                11 - Buscar episódios de uma série a partir de uma data
                
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
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    buscarTop5EpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosPorSerieDepoisDeUmaData();
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

        this.serieBusca =
                this.repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (this.serieBusca.isPresent()) {
            var serieEncontrada = this.serieBusca.get();

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

        this.serieBusca =
                this.repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if(this.serieBusca.isPresent()) {
            System.out.println("Dados da série: " + this.serieBusca.get());
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
                .seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);

        seriesPorTemporadasEAvaliacao.forEach(System.out::println);
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Digite um trecho do episódio que deseja buscar");
        var trechoEpisodio = this.leitura.nextLine();

        List<Episodio> episodiosEncontradosPorTrecho =
                this.repositorio.buscarEpisodiosPorTrecho(trechoEpisodio);

        System.out.println("Episódios encontrados com trecho: " + trechoEpisodio);

        episodiosEncontradosPorTrecho.forEach(e -> {
            System.out.printf("Série: %s Temporada %d - Episódio %d - %s\n",
                    e.getSerie().getTitulo(), e.getTemporada(),
                    e.getNumeroEpisodio(), e.getTitulo());
        });
    }

    private void buscarTop5Episodios() {
        List<Episodio> top5Episodios = this.repositorio.top5Episodios();

        top5Episodios.forEach(e -> {
            System.out.printf("%s - (T%dE%d) %s - Avaliação: %.2f\n",
                    e.getSerie().getTitulo(), e.getTemporada(),
                    e.getNumeroEpisodio(),
                    e.getTitulo(), e.getAvaliacao());
        });
    }

    private void buscarTop5EpisodiosPorSerie() {
        buscarSeriePorTitulo();

        if(this.serieBusca.isPresent()) {
            List<Episodio> top5EpisodiosPorSerie =
                    this.repositorio.top5EpisodiosPorSerie(this.serieBusca.get());

            top5EpisodiosPorSerie.forEach(e -> {
                System.out.printf("(T%dE%d) %s - Avaliação: %.2f\n",
                        e.getTemporada(),
                        e.getNumeroEpisodio(),
                        e.getTitulo(), e.getAvaliacao());
            });
        }
    }

    private void buscarEpisodiosPorSerieDepoisDeUmaData() {
        buscarSeriePorTitulo();

        if(this.serieBusca.isPresent()) {
            System.out.println("Buscar episódios a partir de qual ano? ");
            var anoInicio = this.leitura.nextInt();
            this.leitura.nextLine();

            List<Episodio> episodiosAno =
                    this.repositorio.episodiosPorSerieDepoisDeUmaData(this.serieBusca.get(), anoInicio);

            episodiosAno.forEach(e -> {
                System.out.printf("(T%dE%d) %s - Avaliação: %.2f - Data de " +
                                "lançamento: %tF\n",
                        e.getTemporada(),
                        e.getNumeroEpisodio(),
                        e.getTitulo(), e.getAvaliacao(), e.getDataLancamento());
            });
        }
    }
}