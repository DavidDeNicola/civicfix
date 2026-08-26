import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-register',
  imports: [FormsModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatCardModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  username:string ='';
  email:string ='';
  password:string ='';
  fullName:string ='';
  loading: boolean = false;
  errore: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  register(): void {
    if(!this.username || !this.email || !this.password || !this.fullName){
      this.errore = 'Compila tutti i campi.';
      return;
    }

    this.loading = true;
    this.errore = null;

    const dto = {
      username: this.username,
      email: this.email,
      password: this.password,
      fullName: this.fullName
    }

    this.authService.register(dto).subscribe({
      next: () =>{
        this.loading = false;
        this.router.navigateByUrl('/reports');
      },
      error: (err) => {
        if(err.status === 409) {
          this.errore = 'Username o email già in uso.';
        } else if(err.status === 400) {
          this.errore = 'Dati non validi. Controlla tutti i campi inseriti.';
        } else{
          this.errore = 'Errore durante la registrazione. Riprova più tardi.';
        }
      }
    });
  }

}
