# Lembrete de Remédios

App web instalável (PWA) que avisa nos horários de tomar remédio e guarda o registro de cada dose. Funciona no Chrome do Android e no PC, e abre sem internet depois da primeira vez.

> Este app é apenas um lembrete. Não substitui a orientação do médico ou do farmacêutico.

## Como usar

1. Abra o app no navegador (ou hospede a pasta em um endereço https, como GitHub Pages, Netlify ou Vercel).
2. Toque em **Ativar alertas** e permita as notificações quando o navegador perguntar.
3. Em **Cadastrar remédio**, digite o nome, a dose (opcional) e adicione um ou mais horários com **+ Horário**.
4. Toque em **Salvar remédio**.
5. Na hora certa aparece a tela **Hora do remédio**, com o som e a vibração:
   - **Tomei**: registra a dose como tomada.
   - **Lembrar em 10 min**: adia o aviso.
   - **Pular**: registra que a dose foi pulada.
6. Para instalar no celular: toque nos três pontinhos do Chrome e escolha **Adicionar à tela inicial** (ou **Instalar app**).

## O que faz

- Cadastro de remédios com nome, dose e quantos horários forem necessários.
- Lista **Hoje** com o status de cada dose: Pendente, Tomado, Pulado, Adiado ou Atrasado.
- Alarme em tela cheia com som e vibração, mais notificação do sistema quando permitida.
- Soneca de 10 minutos.
- Histórico dos últimos 7 dias.
- Os dados ficam salvos somente neste aparelho/navegador (localStorage). Nada é enviado para a internet.

## Limites importantes

- **O alarme só toca com o app aberto** (ou com a aba do navegador ativa). Navegadores não permitem alarmes de verdade em segundo plano. Para não esquecer, use também um alarme no próprio celular.
- No Android, o navegador pode bloquear som antes de você tocar em algo na página. Por isso o botão **Ativar alertas** existe: toque nele uma vez ao abrir o app.
- Se o celular for desligado ou o navegador fechar, os horários cadastrados continuam salvos, mas o aviso só volta quando o app for aberto de novo.
- Os dados ficam só no aparelho. Se você limpar os dados do navegador ou trocar de aparelho, eles se perdem.

## Arquivos

| Arquivo | Função |
| --- | --- |
| `index.html` | Estrutura da página e tela de alarme |
| `style.css` | Visual (claro e escuro) |
| `app.js` | Lógica: cadastro, verificação de horários, alarme, histórico |
| `sw.js` | Cache para funcionar sem internet |
| `manifest.webmanifest`, `icon.svg` | Instalação como app |

## Como funciona por dentro

- A cada 10 segundos o app compara a hora atual com os horários cadastrados.
- Cada dose dispara o alarme uma vez por dia. Se já tiver sido registrada (Tomado ou Pulado), não dispara.
- As sonecas ficam guardadas com o horário em que devem voltar, então sobrevivem a um recarregamento da página.
- Ao mudar qualquer arquivo do app, troque `CACHE` em `sw.js` (ex.: `lembrete-remedios-v2`) para os aparelhos atualizarem.

## Como testar

1. Cadastre um remédio com um horário que esteja a 1 ou 2 minutos da hora atual.
2. Aguarde: o alarme deve aparecer em até 10 segundos após o horário.
3. Teste também **Lembrar em 10 min** e confira se o aviso volta.

## Estrutura do projeto

Esta pasta é um projeto independente dentro do repositório `gerador-cartazes`. Não depende de nenhum outro arquivo do repositório.

## Controle de estoque (cartela)

- Ao cadastrar, informe **quantos comprimidos tem** e **quantos vêm por cartela** (padrão: 10). Se deixar em branco, o estoque fica desligado.
- Cada remédio mostra a **cartela desenhada**: as bolinhas coloridas são os comprimidos que restam, e as faixas roxas são as cartelas fechadas.
- Ao marcar uma dose como **Tomada** (na lista ou no alarme), o estoque dá baixa automaticamente. A baixa acontece só uma vez por dose.
- **+ Comprei** soma a quantidade comprada. **Corrigir contagem** define o número exato que restou na caixa.
- O app calcula **para quantos dias dá** (quantidade ÷ número de horários por dia). Com 5 dias ou menos, o cartão fica em vermelho e aparece um aviso.

## Alarme com o app fechado (APK Android)

O APK guarda os dados no Android e agenda cada horário com o `AlarmManager`. Por isso:
- O alarme toca **mesmo com o app fechado**, com o celular bloqueado ou em modo soneca.
- Depois de reiniciar o celular, os alarmes são agendados de novo automaticamente.
- Os botões **Tomei**, **Soneca 10 min** e **Pular** funcionam direto na notificação ou na tela do alarme, e o estoque é atualizado na hora.
- Na primeira abertura, aceite a permissão de notificações. Em alguns celulares (Xiaomi, Samsung, Motorola), também é preciso desligar a **economia de bateria** para o app e permitir **Notificações em tela cheia**.

A versão web (no navegador) continua mostrando o alarme apenas com a página aberta.
