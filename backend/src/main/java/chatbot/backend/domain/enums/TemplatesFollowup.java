package chatbot.backend.domain.enums;

import lombok.Getter;

import java.util.List;

@Getter
public enum TemplatesFollowup {
    studia_perspektywy(List.of(
            "Jaki program jest realizowany na tym kierunku?",
            "Jakie są podstawowe informacje organizacyjne na tym kierunku na Politechnice Łódzkiej?",
            "Jak wygląda rekrutacja na ten kierunek na Politechncie Łódzkiej?",
            "Na czym polegają studia na tym kierunku?",
            "Ile wynosi opłata rekrutacyjna?",
            "Czy w razie odrzucenia aplikacji na kierunek opłata rekrutacyjna jest zwracana?",
            "Czy Politechnika Łódzka oferuje wymiany studenckie?"
    )),
    studia_wymiany(List.of(
            "Jaki jest program na studiach?",
            "Jak wygląda plan zajęć?",
            "Czy studia są stacjonarne czy niestacjonarne?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy zagraniczni studenci mogą ubiegać się o miejsce w akademiku?"
    )),
    studia_rekrutacja(List.of(
            "Jakie kierunki studiów są prowadzone na Wydziale?",
            "W jaki sposób mogę policzyć swój wskaźnik rekrutacyjny",
            "Jakie są perspektywy po ukończeniu kierunku?",
            "Jakie sukcesy osiągają studenci i pracownicy Politechniki Łódzka?",
            "Ile wynosi opłata rekrutacyjna?",
            "Czy w razie odrzucenia aplikacji na kierunek opłata rekrutacyjna jest zwracana?",
            "Czy Politechnika Łódzka oferuje wymiany studenckie?",
            "Ile trwa godzina zajęć na Politechnice Łódzkiej?"
    )),
    organizacja_roku(List.of(
            "Jakie wydziały są na Politechnice Łódzkiej?",
            "Jak zapisać się na zajęcia na Politechnice Łódzkiej?",
            "Jakie kierunki są prowadzone na wydziale?",
            "Czy istnieje możliwość zobaczenia mapy kampusu?",
            "Jak aplikować na Politechnikę Łódzką?",
            "Czy Politechnika Łódzka oferuje wymiany studenckie?"
    )),
    uczelnia_historia(List.of(
            "Jakie wydziały działają na uczelni?",
            "Strategia uczelni?",
            "Kto jest aktualnie prorektorem ds. nauki Politechniki Łódzkiej?",
            "Gdzie znajduje się wydział?",
            "Kto jest aktualnie rektorem Politechniki Łódzkiej"
    )),
    zycie_studenckie_organizacja(List.of(
            "Jak wygląda organizacja roku akademickiego?",
            "Czy na Politechnice Łódzkiej istnieją koła naukowe, kluby lub organizacje studenckie, w których można rozwijać swoje pasje?",
            "Jakie koła naukowe lub organizacje studenckie mają charakter międzywydziałowy na Politechnice Łódzkiej?",
            "Jak mogę dowiedzieć się więcej o działaności tych grup?",
            "Ile studentów uczęszcza/jest na Politechnice Łódzkiej",
            "Czy Politechnika Łódzka ma akademiki dla studentów?"
    )),
    zycie_studenckie_socjalne(List.of(
            "Jakie jest wyposażenie akademików na Politechnice Łódzkiej?",
            "Czy Politechnika Łódzka ma akademiki dla studentów?",
            "Czy w domach studenckich/akademikach jest dostęp do internetu?",
            "Czy w akademikach jest bezpiecznie?",
            "Czy zagraniczni studenci mogą ubiegać się o miejsce w akademiku?",
            "Czy Politechnika Łódzka oferuje wymiany studenckie?"
    )),
    uczelnia_kontakt(List.of(
            "Gdzie znajduje się kampus A na Politechnice Łódzkiej?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie znajduje się Centrum Medyczne/PoliClinic/Lekarz na Politechnice Łódzkiej?",
            "Gdzie znajduje się główna siedziba Politechniki Łódzkiej i jaki jest jej numer telefonu ogólnego kontaktu?",
            "Chciałbym pomocy w innym temacie."
    )),
    organizacja_regulaminy(List.of(
            "Czy kurs z języka angielskiego jest obowiązkowy?",
            "Jakie inne informacje są istotne dla studentów?",
            "Jak uzyskać zaświadczenie o studiowaniu?",
            "Czym są punkty ECTS?",
            "Skąd bierzesz dane?",
            "Co oznacza zaliczenie przedmiotu w regulaminie PŁ?",
            "Kiedy student nabywa prawa studenta na Politechnice Łódzkiej?"
    )),
    uczelnia_wydzialy(List.of(
            "Ile studentów uczęszcza/jest na Politechnice Łódzkiej",
            "Jakie wydziały są na Politechnice Łódzkiej?",
            "Czy istnieje możliwość zobaczenia mapy kampusu?",
            "Ile wynosi opłata rekrutacyjna?",
            "Chciałbym pomocy w innym temacie."
    )),
    Tulbot(List.of(
            "Czy istnieje możliwość zobaczenia mapy kampusu?",
            "Chciałbym pomocy w innym temacie.",
            "Skąd bierzesz dane?",
            "W jakich godzinach odbywają się zajęcia na studiach stacjonarnych?",
            "Czy studia są płatne?"
    )),
    plan_zajec_i_godziny(List.of(
            "Chciałbym pomocy w innym temacie.",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy nagrywanie dźwięku lub obrazu w trakcie zajęć jest dozwolone?",
            "Czy student ma prawo wglądu do swoich ocenionych prac?"
    )),
    organizacja_oceny(List.of(
            "Chciałbym pomocy w innym temacie.",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy praca zawodowa może zostać uznana jako praktyka lub jej część?",
            "Czy student ma prawo wglądu do swoich ocenionych prac?",
            "Jak zalicza się praktyki i kto o tym decyduje?",
            "Czy można rozpocząć praktyki wcześniej niż przewiduje program?"
    )),
    studia_praktyki(List.of(
            "Czy można rozpocząć praktyki wcześniej niż przewiduje program?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy praca zawodowa może zostać uznana jako praktyka lub jej część?",
            "Jak zalicza się praktyki i kto o tym decyduje?",
            "Co grozi za niesamodzielną pracę oddaną do oceny (np. projekt, sprawozdanie)?"
    )),
    organizacja_urlopy(List.of(
            "Jakie są podstawowe zasady urlopu: prawa studenta, opłaty i formalności po urlopie?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Jakie ograniczenia dotyczą urlopu długoterminowego w PŁ?",
            "Chciałbym pomocy w innym temacie.",
            "Jakie ograniczenia dotyczą urlopu długoterminowego w PŁ?"
    )),
    organizacja_egzaminy_dyplomowe(List.of(
            "Jak wygląda recenzowanie pracy dyplomowej i co się dzieje przy negatywnej ocenie recenzenta?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Gdzie mogę sprawdzić harmonogram i terminy obligatoryjne?",
            "Chciałbym pomocy w innym temacie."
    )),
    uczelnia_jednostki(List.of(
            "Chciałbym pomocy w innym temacie.",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Kiedy powstała Politechnika Łódzka?",
            "Kto jest aktualnie rektorem Politechniki Łódzkiej?"
    )),
    infrastruktura_it(List.of(
            "Czy chcesz dowiedzieć się więcej na ten temat?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czym jest wikamp?",
            "Chciałbym pomocy w innym temacie.",
            "Gdzie można sprawdzić oceny końcowe?"
    )),
    kierunki_organizacja(List.of(
            "Czy chcesz dowiedzieć się więcej na ten temat?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Jak zapisać się na zajęcia na Politechnice Łódzkiej?",
            "Chciałbym pomocy w innym temacie.",
            "Jakie są perspektywy po ukończeniu kierunku"
    )),
    studia_program(List.of(
            "Jak wygląda rekrutacja na tym kierunku?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Jak wygląda program studiów na kierunku",
            "Chciałbym pomocy w innym temacie.",
            "Jakie są podstawowe informacje organizacyjne o kierunku",
            "Jakie są perspektywy po ukończeniu kierunku"
    )),
    studia_kierunki(List.of(
            "Czy chcesz dowiedzieć się więcej na ten temat?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy masz pytania dotyczące studiów?",
            "Chciałbym pomocy w innym temacie.",
            "Jakie są podstawowe informacje organizacyjne o kierunku",
            "Jakie są perspektywy po ukończeniu kierunku"
    )),
    zycie_studenckie_organizacje(List.of(
            "Czy chcesz dowiedzieć się więcej na ten temat?",
            "Jakie inne informacje są istotne dla studentów?",
            "Jakie korzyści płyną z uczestnictwa w życiu studenckim na Politechnice Łódzkiej",
            "Gdzie można znaleźć więcej szczegółów?",
            "Czy Politechnika Łódzka ma akademiki dla studentów?",
            "Chciałbym pomocy w innym temacie.",
            "Jakie koła naukowe, kluby lub organizacje studenckie działają na Wydziale Mechanicznym?",
            "Czy istnieje możliwość zdalnego uczestnictwa w zajęciach na Politechnice Łódzkiej?"
    )),
    ogolne(List.of(
            "Chcę dowiedzieć się więcej na ten temat?",
            "Jakie inne informacje są istotne dla studentów?",
            "Gdzie można znaleźć więcej szczegółów?",
            "Kim jesteś i co robisz/jakie są twoje obowiązki?",
            "Chciałbym pomocy w innym temacie?"
    ));

    private final List<String> templates;

    TemplatesFollowup(List<String> templates) {
        this.templates = templates;
    }

    public static TemplatesFollowup fromCategory(String category) {
        if (category == null || category.isBlank()) {
            return ogolne;
        }
        try {
            return TemplatesFollowup.valueOf(category.trim().toLowerCase());
        } catch (IllegalArgumentException e) {
            return ogolne;
        }
    }
}

