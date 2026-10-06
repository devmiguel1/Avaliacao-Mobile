# WaterApp: Avaliação N1

App Android (Java) para controle do consumo diário de água, com a view customizada `WaterProgressView`.

## Como colocar no Android Studio

1. **New Project → Empty Views Activity**
   - Name: `WaterApp`
   - Package name: `com.example.waterapp`
   - Language: **Java**
   - Minimum SDK: 24 ou superior
2. Copie os arquivos desta pasta para o projeto (substitua se já existirem):

| Arquivo | Destino no projeto |
|---|---|
| `app/src/main/java/com/example/waterapp/MainActivity.java` | `app/src/main/java/com/example/waterapp/` (substitui) |
| `app/src/main/java/com/example/waterapp/WaterProgressView.java` | mesma pasta (novo) |
| `app/src/main/java/com/example/waterapp/WaterStorage.java` | mesma pasta (novo) |
| `app/src/main/res/values/attrs.xml` | `app/src/main/res/values/` (novo) |
| `app/src/main/res/layout/activity_main.xml` | `app/src/main/res/layout/` (substitui) |
| `app/src/main/res/layout/item_record.xml` | `app/src/main/res/layout/` (novo) |

3. Rode o app (**Run ▶**).

> Se o seu pacote não for `com.example.waterapp`, troque a linha `package ...` nos 3 arquivos `.java` e o nome do pacote na tag `<com.example.waterapp.WaterProgressView>` do `activity_main.xml`.
> Opcional: para evitar perder o modo de exibição ao girar a tela, adicione `android:screenOrientation="portrait"` na `<activity>` do `AndroidManifest.xml`.

## Persistência escolhida: SharedPreferences + JSON

Os dados são poucos e simples: uma meta, um acumulado, uma data e uma lista curta de registros do dia. Não há consultas complexas nem relacionamentos, então SQLite/Room seria mais código sem benefício. O SharedPreferences é nativo, não exige dependências e mantém os dados após fechar o app. Os registros são salvos como texto JSON (`org.json`, que já vem no Android).

Chaves salvas: `goal` (meta), `date` (dia dos registros), `accumulated` (acumulado) e `records` (lista de volume + instante).

## Onde cada requisito está

| Requisito do enunciado | Onde |
|---|---|
| View customizada com Canvas | `WaterProgressView.onDraw` |
| Atributos XML com `TypedArray` | `attrs.xml` + construtor da `WaterProgressView` |
| `setProgress(int)` com animação suave | `WaterProgressView.setProgress` e `animateTo` (`ValueAnimator`) |
| Passa de 100% sem travar | arco limitado a 360°, texto mostra o valor real (ex.: 120%) |
| Alerta na View | arco e texto em vermelho, 2ª volta em vermelho escuro e "Meta ultrapassada!" |
| Alerta na tela | `tvAlert` (banner) e acumulado em vermelho, em `MainActivity.refresh()` |
| Alerta baseado nos valores salvos | `acc > goal` calculado a partir do `WaterStorage` |
| Clique alterna percentual / absoluto | `setOnClickListener` no construtor da View (`showAbsolute`) |
| Alterar a meta em execução | `setMaxValue()` + botão "Definir meta" |
| Registros do dia e novo ciclo por data | `WaterStorage.checkNewDay()` (compara com a data do sistema) |
| Alterar meta preservando registros | `setGoal()` só mexe na chave `goal` |
| Validação de entradas | `MainActivity.readValue()` (vazio, zero, negativo, texto, valor muito alto) |

## Roteiro de testes

1. Definir meta 2000, registrar 500, 800, 600: deve mostrar 95%. Clicar no gráfico: "1900/2000 ml".
2. Registrar mais 300: acumulado 2200, 110%, tudo em vermelho e banner de alerta.
3. Alterar a meta para 3000: o alerta some, e os registros continuam.
4. Fechar o app (remover dos recentes) e reabrir: meta, lista e acumulado voltam.
5. Mudar a data do aparelho para o dia seguinte e reabrir: registros zerados, meta mantida.
6. Tentar registrar vazio, 0 ou 99999: deve mostrar aviso e não registrar.

## Perguntas prováveis na defesa

- **Por que `invalidate()`?** Marca a View como "suja", e o Android chama `onDraw` de novo.
- **Por que os `Paint` ficam fora do `onDraw`?** O `onDraw` roda dezenas de vezes por segundo na animação, e criar objetos ali gera lixo de memória e travadas.
- **Por que `a.recycle()`?** O `TypedArray` é compartilhado e reaproveitado pelo sistema.
- **Como o progresso passa de 100%?** O arco é limitado a 360°, mas o percentual é calculado como `consumo * 100 / meta` sem limite.
- **Como funciona a animação?** O `ValueAnimator` interpola `shownPercent` do valor atual até o novo alvo e chama `invalidate()` a cada quadro.
- **Como o app sabe que é um novo dia?** Salva a data (`yyyy-MM-dd`) junto aos registros. Em `onResume` e antes de cada registro compara com a data atual do sistema. Se mudou, zera registros e acumulado e mantém a meta.
- **Por que o alerta não é só visual?** A condição `acumulado > meta` é recalculada a cada `refresh()` a partir dos valores persistidos. Ao reabrir o app, o alerta reaparece sozinho.
